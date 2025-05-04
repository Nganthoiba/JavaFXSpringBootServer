package com.javafxserver.digitalsigner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import com.javafxserver.service.TokenService;
import org.apache.pdfbox.Loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class PDFSigner {

    // This method signs a PDF with the ePass token private key
    public static byte[] signPDF(File originalFile, String pin, TokenService tokenService, SignatureDetail signDetail) throws Exception {
    	//String pdfFilePath
        PDDocument document;
    	// Load the PDF document
        
        try{
        	//originalFile = new File(pdfFilePath);       	
        	document = Loader.loadPDF(originalFile);
        }
        catch (IOException e) {
        	System.out.println("Error loading PDF file " + e.getMessage());
			throw new Exception("Error loading PDF file " + e.getMessage(), e);
        }
                
        
        // Create a signature field (optional, for visual representation)
        String signerName = "";
        List<CertificateInfo> certs = tokenService.getCertificateDetails();
        for (CertificateInfo info : certs) {
        	signerName = info.getSubjectDetails("CN");
        }
        String location = signDetail != null?signDetail.location:"";
        float x = signDetail != null? signDetail.coordinate.X:360;
        float y = signDetail != null? signDetail.coordinate.Y:150;
        
      
        //Get the first page usually
        PDPage page = document.getPage(0);
        // Define rectangle for signature appearance
        PDRectangle rect = new PDRectangle(x, y, 200, 50); // x, y, width, height 
        
        // Initialize the signature object
        PDSignature signature = getSignature(signerName, location);
        
        addSignatureField(document, 1, rect, signature);
        
        drawSignatureText(document, page, rect, signerName, location);
        
        //contentStream.close();        
        
        // Add signature to the document
        document.addSignature(signature, new EpassSignature(tokenService.getTokenDetails(), pin));
        
        /**
         * When saving the signed document, I use incremental saving to preserve existing content and signatures
         */
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.saveIncremental(outputStream);
            return outputStream.toByteArray();
        }        
        
    }  
    
    private static PDSignature getSignature(String signerName, String location) {
    	PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        //signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_SHA1);
        signature.setName(signerName);
        signature.setLocation(location);        
        signature.setSignDate(Calendar.getInstance());
        return signature;
    }
    
    private static void addSignatureField(PDDocument document, int pageNo, PDRectangle rect, PDSignature signature) throws IOException {
    	// Ensure AcroForm exists
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }
    	
    	PDPage page = document.getPage(pageNo-1);
    	
    	// Generate a unique field name
        String uniqueFieldName = "Signature_" + System.currentTimeMillis();

        // Create and name the signature field
        PDSignatureField sigField = new PDSignatureField(acroForm);
        sigField.setPartialName(uniqueFieldName); // name of the field

        // Create a widget annotation to represent the signature visually
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(rect);
        widget.setPage(page);

        // Link widget to the signature field
        sigField.setWidgets(List.of(widget));

        // Add annotation to the page
        page.getAnnotations().add(widget);

        // Add the signature field to the form
        acroForm.getFields().add(sigField);
        
        // Associate the signature with the signature field
        sigField.setValue(signature);
    }
    
    private static void drawSignatureText(PDDocument document, PDPage page, PDRectangle rect, String signerName, String location) throws IOException, URISyntaxException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            contentStream.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
            contentStream.beginText();

            PDType0Font font = PDType0Font.load(document, FontLoader.getFontFile("Roboto-Regular.ttf"));
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(rect.getLowerLeftX() + 5, rect.getUpperRightY() - 15);

            contentStream.showText("Digitally signed by: " + signerName);
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Location: " + location);
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));

            contentStream.endText();
        }
    }
    
}
