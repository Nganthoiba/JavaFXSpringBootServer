package com.javafxserver.digitalsigner;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.apache.pdfbox.Loader;

import java.io.*;
import com.javafxserver.config.Config;
import com.javafxserver.exceptions.SunPKCS11NotFoundException;

import java.lang.reflect.Constructor;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Collections;

public class Epass2003PDFSigner {
	public static void signPDF(File pdfFile, File signedPdf, String pkcs11LibraryPath, String pin) throws Exception {
        // Load the PKCS#11 module
		/*
        Provider base = Security.getProvider("SunPKCS11");
        if (base == null) {
            throw new SunPKCS11NotFoundException("SunPKCS11 not available in current JDK.");
        }
        
        File configFile = Config.createTemporaryPKCS11Config();
        Provider pkcs11Provider = base.configure(configFile.toString());
			*/
		Provider pkcs11Provider;
		try {
            Class<?> pkcs11Class = Class.forName("sun.security.pkcs11.SunPKCS11");
            Constructor<?> constructor = pkcs11Class.getConstructor(String.class);
            pkcs11Provider = (Provider) constructor.newInstance(pkcs11LibraryPath);
            Security.addProvider(pkcs11Provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load PKCS#11 provider", e);
        }
        // Load the PKCS#11 keystore
        KeyStore keystore = KeyStore.getInstance("PKCS11", pkcs11Provider);
        keystore.load(null, pin.toCharArray());

        // Get the private key and certificate
        String alias = keystore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, pin.toCharArray());
        Certificate cert = keystore.getCertificate(alias);

        // Load the PDF
        PDDocument document = Loader.loadPDF(pdfFile);
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("ePass2003 Signer");
        signature.setLocation("India");
        signature.setReason("Document Signed");

        SignatureOptions options = new SignatureOptions();
        options.setPage(0); // Set the page where the signature should appear (0 = first page)

        document.addSignature(signature, new SignatureInterface() {
            @Override
            public byte[] sign(InputStream content) throws IOException {
                try {
                    return createPKCS7Signature(content, pkcs11Provider, privateKey, cert);
                } catch (Exception e) {
                	e.printStackTrace();
                    throw new IOException("Error signing PDF: " + e.getMessage(), e);
                }
            }
        }, options);

        // Save the signed document
        try (FileOutputStream fos = new FileOutputStream(signedPdf)) {
            document.saveIncremental(fos);
        }

        document.close();
    }

    private static byte[] createPKCS7Signature(InputStream content,Provider pkcs11Provider,  PrivateKey privateKey, Certificate cert) throws Exception {
        // Create CMS Signed Data
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").setProvider(pkcs11Provider).build(privateKey);
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                        .build(contentSigner, (java.security.cert.X509Certificate) cert)
        );
        generator.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

        // Generate detached signature
        byte[] contentBytes = content.readAllBytes();
        CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(contentBytes), false);
        return signedData.getEncoded();
    }
}
