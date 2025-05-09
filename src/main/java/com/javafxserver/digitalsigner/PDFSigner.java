package com.javafxserver.digitalsigner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import com.javafxserver.service.TokenService;
import org.apache.pdfbox.Loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class PDFSigner {

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
        String signerName = certs.isEmpty()?"":certs.get(0).getSubjectDetails("CN");        
        
        //Get the first page usually
        PDPage page = document.getPage(signDetail.pageNumber - 1);
        
        // Define rectangle for signature appearance
        
        PDRectangle rect = new PDRectangle(signDetail.coordinate.X, 
        		signDetail.coordinate.Y, 
        		signDetail.rectangle.width, 
        		signDetail.rectangle.height); // x, y, width, height 
        
        drawSignatureText(document, page, rect, signerName, signDetail);  
        
        // Initialize the signature object
        PDSignature signature = getSignature(signerName, signDetail);  
        
        addSignatureField(document, page, rect, signature);   
        
        SignatureOptions options = new SignatureOptions();
        options.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE); // you can adjust size if needed
        
        // Add signature to the document
        document.addSignature(signature, new EpassSignature(tokenService.getTokenDetails(), pin), options);
        
        /**
         * When saving the signed document, I use incremental saving to preserve existing content and signatures
         */
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.saveIncremental(outputStream);            
            return outputStream.toByteArray();
        }        
        
    }  
    
    private static PDSignature getSignature(String signerName, SignatureDetail signDetail) 
    		throws NoSuchAlgorithmException {
    	PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        //signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_SHA1);
        signature.setName(signerName);
        signature.setLocation(signDetail.location); 
        signature.setReason(signDetail.reason);
        signature.setSignDate(Calendar.getInstance());
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
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }    	
        
    	        
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
    
    private static void drawSignatureText(PDDocument document, PDPage page, PDRectangle rect, String signerName, SignatureDetail signDetail) 
    		throws IOException, URISyntaxException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
        	
        	// Set the stroke color (black)
            PDColor black = new PDColor(new float[]{0.5f, 0.5f, 0.5f}, PDDeviceRGB.INSTANCE);  // Black color
            contentStream.setStrokingColor(black);
        	
        	contentStream.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
        	// Stroke the path (draw the border)
            contentStream.stroke();
            
            contentStream.beginText();
            
            PDType0Font font = PDType0Font.load(document, FontLoader.getFontFile("Roboto-Regular.ttf"));
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(rect.getLowerLeftX() + 5, rect.getUpperRightY() - 15);

            contentStream.showText("Digitally signed by:");
            contentStream.newLineAtOffset(0, -12);
            
            contentStream.showText(signerName);
            contentStream.newLineAtOffset(0, -12);
            
            contentStream.showText("Location: " + signDetail.location);
            
            contentStream.newLineAtOffset(0, -12);
            contentStream.showText("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(Calendar.getInstance().getTime()));

            contentStream.endText();
        }
    }
    
}
