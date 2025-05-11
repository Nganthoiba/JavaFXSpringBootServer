package com.javafxserver.digitalsigner;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.util.Matrix;

import com.javafxserver.service.TokenService;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PDFSigner {

    public static byte[] signPDF(File originalFile, String pin, TokenService tokenService, SignatureDetail signDetail) throws Exception {
        try (PDDocument document = Loader.loadPDF(originalFile)) {
            validatePageNumber(document, signDetail.pageNumber);

            // Fetch signer details
            populateSignerDetails(tokenService, signDetail);

            // Define signature rectangle
            PDRectangle rect = new PDRectangle(signDetail.coordinate.X, signDetail.coordinate.Y, signDetail.rectangle.width, signDetail.rectangle.height);

            // Draw signature text
            drawSignatureText(document, document.getPage(signDetail.pageNumber - 1), rect, signDetail);

            // Add signature field
            PDSignature signature = createSignature(signDetail);
            addSignatureField(document, document.getPage(signDetail.pageNumber - 1), rect, signature);

            // Set visual signature
            SignatureOptions signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(16384); // 16KB
            signatureOptions.setVisualSignature(createVisualSignatureTemplate(document, signDetail, rect));
            signatureOptions.setPage(signDetail.pageNumber - 1);

            // Sign the document
            document.addSignature(signature, new EpassSignature(tokenService.getTokenDetails(), pin), signatureOptions);

            // Save incrementally
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.saveIncremental(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    private static void validatePageNumber(PDDocument document, int pageNumber) {
        if (pageNumber > document.getNumberOfPages()) {
            throw new IndexOutOfBoundsException("Page number " + pageNumber + " exceeds total pages " + document.getNumberOfPages());
        }
    }

    private static void populateSignerDetails(TokenService tokenService, SignatureDetail signDetail) throws Exception {
        List<CertificateInfo> certs = tokenService.getCertificateDetails();
        signDetail.signerName = certs.isEmpty() ? "" : certs.get(0).getSubjectDetails("CN");
        signDetail.signerEmail = certs.isEmpty() ? "" : certs.get(0).getSubjectDetails("EMAILADDRESS");
    }

    private static PDSignature createSignature(SignatureDetail signDetail) {
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName(signDetail.signerName);
        signature.setContactInfo(signDetail.signerEmail);
        signature.setLocation(signDetail.location);
        signature.setReason(signDetail.reason);
        signature.setSignDate(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        return signature;
    }

    private static void addSignatureField(PDDocument document, PDPage page, PDRectangle rect, PDSignature signature) throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }

        PDSignatureField sigField = new PDSignatureField(acroForm);
        sigField.setPartialName("Signature_" + UUID.randomUUID());

        // Create a widget annotation to represent the signature visually
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(rect);
        widget.setPage(page);
        widget.setPrinted(true);
        
        // Link widget to the signature field
        sigField.setWidgets(List.of(widget));
        
        // Add annotation to the page
        page.getAnnotations().add(widget);
        
        // Add the signature field to the form
        acroForm.getFields().add(sigField);
        
        // Associate the signature with the signature field
        sigField.setValue(signature);
    }

    private static void drawSignatureText(PDDocument document, PDPage page, PDRectangle rect, SignatureDetail signDetail) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            contentStream.setStrokingColor(Color.GRAY);
            contentStream.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
            contentStream.stroke();

            contentStream.beginText();
            PDType0Font font = PDType0Font.load(document, FontLoader.getFontFile("Roboto-Regular.ttf"));
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(rect.getLowerLeftX() + 5, rect.getUpperRightY() - 15);
            contentStream.showText("Digitally signed by:");
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText(signDetail.signerName);
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Location: " + signDetail.location);
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Date: " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a").format(Calendar.getInstance().getTime()));
            contentStream.endText();
        }
    }

    // create a template PDF document with empty signature and return it as a stream.
    private static InputStream createVisualSignatureTemplate(PDDocument srcDoc, SignatureDetail signatureDetail, PDRectangle rect) 
    		throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
        	int pageNum = signatureDetail.pageNumber - 1;
            PDPage page = new PDPage(srcDoc.getPage(pageNum).getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);
            acroFormFields.add(signatureField);

            widget.setRectangle(rect);

            // from PDVisualSigBuilder.createHolderForm()
            PDStream stream = new PDStream(doc);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources res = new PDResources();
            form.setResources(res);
            form.setFormType(1);
            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            switch (srcDoc.getPage(pageNum).getRotation())
            {
                case 90:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(1));
				break;
                case 180:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(2)); 
                    break;
                case 270:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(3));
				break;
                case 0:
                default:
                    break;
            }
            form.setBBox(bbox);
            
            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }

}
