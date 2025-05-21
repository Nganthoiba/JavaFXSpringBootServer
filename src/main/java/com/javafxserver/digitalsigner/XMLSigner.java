package com.javafxserver.digitalsigner;

import java.io.File;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.*;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.List;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import com.javafxserver.service.TokenService;

public class XMLSigner {

    public static byte[] signXML(File xmlFile, TokenService tokenService) throws Exception {
    	// The XML file is parsed and a document is prepared for signing.
    	Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(xmlFile);
        return signDocument(doc, tokenService, true); // Save to signed_output.xml
    }

    public static byte[] signXML(String xmlData, TokenService tokenService) throws Exception {
        // The XML string data is parsed and a document is prepared for signing.
    	Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlData.getBytes()));
        
        return signDocument(doc, tokenService, false); // No file output
    }

    private static byte[] signDocument(Document doc, TokenService tokenService, boolean saveToFile) throws Exception {
        doc.setXmlStandalone(true);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        // Initialize XML Signature Factory to create the digital signature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Prepare digest and transforms
        // A Reference object is created to define the digest method (SHA-256) and the transform (ENVELOPED) for the signature.
        Reference ref = fac.newReference(
                "",
                fac.newDigestMethod(DigestMethod.SHA256, null),
                List.of(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                null
        );
        
        // A SignedInfo object is created, specifying the canonicalization method and the signature method (RSA_SHA256).
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                List.of(ref)
        );
        
        
        // Retrieve private key and certificate chain from the TokenService
        PrivateKey privateKey = tokenService.getPrivateKey();
        List<X509Certificate> chertChain = tokenService.getX509CertificateChain(); 

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        KeyInfo ki = kif.newKeyInfo(List.of(kif.newX509Data(chertChain)));

        // Create the XMLSignature
        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        // Output result to ByteArrayOutputStream
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        trans.transform(new DOMSource(doc), new StreamResult(outputStream));

        if (saveToFile) {
            trans.transform(new DOMSource(doc), new StreamResult(new File("signed_output.xml")));
        }

        return outputStream.toByteArray();
    }
}