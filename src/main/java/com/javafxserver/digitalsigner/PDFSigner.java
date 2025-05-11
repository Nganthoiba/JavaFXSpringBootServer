package com.javafxserver.digitalsigner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import com.javafxserver.service.TokenService;
import org.apache.pdfbox.Loader;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class PDFSigner {	
	
	private static final int SIGNATURE_FONT_SIZE = 9;
	private static final int PREFERRED_SIGNATURE_SIZE = 16384; // 16KB

    // This method signs a PDF with the ePass token private key
    public static byte[] signPDF(File originalFile, String pin, TokenService tokenService, SignatureDetail signDetail) throws Exception {
    	
        PDDocument document;
    	// Load the PDF document        
        try{       	
        	document = Loader.loadPDF(originalFile);
        	if(signDetail.pageNumber > document.getNumberOfPages()) {
        		throw new IndexOutOfBoundsException("Page number " + signDetail.pageNumber +
        		        " exceeds total pages " + document.getNumberOfPages());
        	}
        }
        catch (IOException e) {
        	System.out.println("Error loading PDF file " + e.getMessage());
			throw new Exception("Error loading PDF file " + e.getMessage(), e);
        }
                
        
        // Create a signature field (optional, for visual representation)
        
        List<CertificateInfo> certs = tokenService.getCertificateDetails();        
        signDetail.signerName = certs.isEmpty()?"":certs.get(0).getSubjectDetails("CN");
        signDetail.signerEmail = certs.isEmpty()?"":certs.get(0).getSubjectDetails("EMAILADDRESS");
        
        
        //Get the first page usually
        PDPage page = document.getPage(signDetail.pageNumber - 1);
        
        // Define rectangle for signature appearance
        
        PDRectangle rect = new PDRectangle(signDetail.coordinate.X, 
        		signDetail.coordinate.Y, 
        		signDetail.rectangle.width, 
        		signDetail.rectangle.height); // x, y, width, height 
        
        drawSignatureText(document, page, rect, signDetail);  
        
        // Initialize the signature object
        PDSignature signature = getSignature(signDetail);  
        
        addSignatureField(document, page, rect, signature);   
        
        SignatureOptions signatureOptions = new SignatureOptions();
        
        signatureOptions.setPreferredSignatureSize(PREFERRED_SIGNATURE_SIZE);
        signatureOptions.setVisualSignature(createVisualSignatureTemplate(document, signDetail, rect));
        signatureOptions.setPage(signDetail.pageNumber - 1);
        
        // Add signature to the document
        document.addSignature(signature, new EpassSignature(tokenService.getTokenDetails(), pin), signatureOptions);
        
        /**
         * When saving the signed document, I use incremental saving to preserve existing content and signatures
         */
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.saveIncremental(outputStream);            
            return outputStream.toByteArray();
        }        
        
    }  
    
    private static PDSignature getSignature(SignatureDetail signDetail) 
    		throws NoSuchAlgorithmException {
    	PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        //signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_SHA1);
        signature.setName(signDetail.signerName);
        signature.setContactInfo(signDetail.signerEmail);
        signature.setLocation(signDetail.location); 
        signature.setReason(signDetail.reason);
        
        //signature.setSignDate(Calendar.getInstance());
        Calendar signDate = Calendar.getInstance();
        signDate.setTimeZone(TimeZone.getTimeZone("UTC")); // Use UTC for consistency
        signature.setSignDate(signDate);

        return signature;
    }
    
    /**
     * The method addSignatureField() in the code is responsible for creating and adding a signature field to the PDF document. 
     * This field is associated with the visual representation of a digital signature, which allows the PDF to hold the signature 
     * data and visually display a signature widget (like a rectangle where the signature will appear).
     * 
     * @param document
     * @param pageNo
     * @param rect
     * @param signature
     * @throws IOException
     */    
    private static void addSignatureField(PDDocument document, PDPage page, PDRectangle rect, PDSignature signature) throws IOException {
    	// Ensure AcroForm exists
    	
    	PDAcroForm acroForm = ensureAcroForm(document);
    	        
        // Generate a unique and descriptive field name
        String uniqueFieldName = "Signature_Page" + (document.getPages().indexOf(page) + 1) + "_" + java.util.UUID.randomUUID();

        // Check if a field with the same name already exists
        if (acroForm.getField(uniqueFieldName) != null) {
            throw new IOException("A field with the name '" + uniqueFieldName + "' already exists.");
        }
                
        // Create and name the signature field
        PDSignatureField sigField = new PDSignatureField(acroForm);
        sigField.setPartialName(uniqueFieldName); // name of the field

        // Create a widget annotation to represent the signature visually
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(rect);
        widget.setPage(page);
        widget.setPrinted(true); // Ensuring it appears in print

        // Link widget to the signature field
        sigField.setWidgets(List.of(widget));

        // Add annotation to the page
        page.getAnnotations().add(widget);

        // Add the signature field to the form
        acroForm.getFields().add(sigField);
        
        // Associate the signature with the signature field
        sigField.setValue(signature);
    }
    
    private static void drawSignatureText(PDDocument document, PDPage page, PDRectangle rect, SignatureDetail signDetail) 
    		throws IOException, URISyntaxException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
        	
        	// Set the stroke color (black)
            PDColor black = new PDColor(new float[]{0.5f, 0.5f, 0.5f}, PDDeviceRGB.INSTANCE);  // Black color
            contentStream.setStrokingColor(black);
        	
        	contentStream.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
        	// Stroke the path (draw the border)
            contentStream.stroke();
            
            contentStream.beginText();
            
            PDType0Font font = PDType0Font.load(document, FontLoader.getFontFile("Roboto-Regular.ttf"));//getFont(document);
            
            contentStream.setFont(font, SIGNATURE_FONT_SIZE);
            contentStream.newLineAtOffset(rect.getLowerLeftX() + 5, rect.getUpperRightY() - 15);
            
            //float lineHeight = font.getFontDescriptor().getCapHeight() / 1000 * SIGNATURE_FONT_SIZE;
            float lineHeight = 12f;
            contentStream.showText("Digitally signed by:");
            contentStream.newLineAtOffset(0, -lineHeight);
            
            contentStream.showText(signDetail.signerName);
            contentStream.newLineAtOffset(0, -lineHeight);
            
            contentStream.showText("Location: " + signDetail.location);
            
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Date: " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a").format(Calendar.getInstance().getTime()));

            contentStream.endText();
        }
    }
    
    // create a template PDF document with empty signature and return it as a stream.
    private static InputStream createVisualSignatureTemplate(PDDocument srcDoc, SignatureDetail signatureDetail, PDRectangle rect) throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
        	int pageNum = signatureDetail.pageNumber - 1;
            PDPage page = new PDPage(srcDoc.getPage(pageNum).getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = ensureAcroForm(doc);
            
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            
            acroFormFields.add(signatureField);

            widget.setRectangle(rect);
            
            PDStream stream = new PDStream(doc);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources res = new PDResources();
            form.setResources(res);
            form.setFormType(1);
            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            int rotation = srcDoc.getPage(pageNum).getRotation();
            if (rotation % 90 == 0) {
                form.setMatrix(AffineTransform.getQuadrantRotateInstance(rotation / 90));
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
    
    private static PDAcroForm ensureAcroForm(PDDocument doc) {
        PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
        }
        acroForm.setSignaturesExist(true);
        acroForm.setAppendOnly(true);
        acroForm.getCOSObject().setDirect(true);
        return acroForm;
    }
    
}