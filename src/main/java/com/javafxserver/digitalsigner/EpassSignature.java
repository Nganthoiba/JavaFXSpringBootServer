package com.javafxserver.digitalsigner;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Date;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SimpleAttributeTableGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.x509.Time;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;

import javax.security.auth.x500.X500Principal; // For issuer from X509Certificate
import org.bouncycastle.asn1.x500.X500Name;     // For conversion to Bouncy Castle X500Name

//SignatureInterface implementation to handle the signing process
public class EpassSignature implements SignatureInterface{
	public final TokenDetails tokenDetails;
    public final String pin;

    public EpassSignature(TokenDetails tokenDetails, String pin) {
        this.tokenDetails = tokenDetails;
        this.pin = pin;
    }
    
    @Override
    public byte[] sign(InputStream content) throws IOException {  
		
		try {
			// Read content into a byte array
			byte[] contentBytes = content.readAllBytes();
			Provider pkcs11Provider = tokenDetails.getPkcs11Provider();
			
			// Create a CMSProcessable for the content
			CMSProcessableByteArray cmsData = new CMSProcessableByteArray(contentBytes);
			Enumeration<String> aliases = tokenDetails.getKeyStore().aliases();
			
			String alias = aliases.hasMoreElements() ? aliases.nextElement() : null;
			if (alias == null) {
				throw new IOException("No certificates found in token.");
			}
			
            // Get private key and certificate from token
            PrivateKey privateKey = (PrivateKey) tokenDetails.getKeyStore().getKey(alias, pin.toCharArray());
            X509Certificate cert = (X509Certificate) tokenDetails.getKeyStore().getCertificate(alias);
                        
            /*
            //Debugging 
            System.out.println("Using provider: " + pkcs11Provider.getName());
            System.out.println("Signing with key: " + privateKey.getAlgorithm());
            System.out.println("Certificate: " + cert.getSubjectX500Principal().getName());
            System.out.println("Certificate String: " + cert.toString());
            */
            // Certificate chain as a list
            Certificate[] certChain = tokenDetails.getKeyStore().getCertificateChain(alias);
            
            if(certChain == null || certChain.length == 0) {
				throw new IOException("No certificate chain found in token.");
			}
            
            System.out.println("Certificate chain length: " + certChain.length);
            
            List<Certificate> fullChain = new ArrayList<>();
            fullChain.addAll(Arrays.asList(certChain));
            //List<Certificate> certList = Arrays.asList(certChain);
            JcaCertStore certStore = new JcaCertStore(fullChain);

            // Build CMS/PKCS#7 Signature
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
            		.setProvider(pkcs11Provider)
            		.build(privateKey);
            
            // Create a CMSSignedDataGenerator
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().build()
                )
                .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(getSignedAttribute(cert)))
                .build(contentSigner, cert)
            );
            generator.addCertificates(certStore);

            // Generate signed data (detached = true)
            CMSSignedData signedData = generator.generate(cmsData, false);
            return signedData.getEncoded();
        } catch (Exception e) {
			throw new IOException("Error in retrieving private key or certificate: " + e.getMessage(), e);
		}            
    }
    
    
    //Get Signed Attribute
    private AttributeTable getSignedAttribute(X509Certificate cert) 
    		throws CertificateEncodingException, NoSuchAlgorithmException {
    	Hashtable<ASN1ObjectIdentifier, Attribute> attrMap =
    	        new Hashtable<>();
    	
    	// signing-time
    	Attribute signingTimeAttr = new Attribute(
    	        PKCSObjectIdentifiers.pkcs_9_at_signingTime,
    	        new DERSet( new Time( new Date() ) )
    	);

    	// content-type: always data
    	Attribute contentTypeAttr = new Attribute(
    	        PKCSObjectIdentifiers.pkcs_9_at_contentType,
    	        new DERSet(PKCSObjectIdentifiers.data)
    	);
    	Attribute signingCertificateV2 = getSigningCertificateV2(cert);
    	

    	// These will be merged with automatically computed messageDigest
    	attrMap.put(contentTypeAttr.getAttrType(), contentTypeAttr);
    	attrMap.put(signingTimeAttr.getAttrType(), signingTimeAttr);
    	attrMap.put(signingCertificateV2.getAttrType(), signingCertificateV2);

    	// Optional: you can add others if needed (like signing certificate)
    	AttributeTable signedAttributes = new AttributeTable(attrMap);
		return signedAttributes;
	}
    
    public Attribute getSigningCertificateV2(X509Certificate cert) 
    		throws CertificateEncodingException, NoSuchAlgorithmException {
    	// signingCertificateV2 (RFC 5035)
        byte[] certHash = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());

        // Get the issuer name and serial number
        X500Principal issuerPrincipal = cert.getIssuerX500Principal();
        X500Name issuerName = new X500Name(issuerPrincipal.getName());  // Convert to X500Name

        GeneralName issuerGeneralName = new GeneralName(GeneralName.directoryName, issuerName);

        // Construct issuerSerial correctly using GeneralNames
        IssuerSerial issuerSerial = new IssuerSerial(
            new GeneralNames(issuerGeneralName),
            new ASN1Integer(cert.getSerialNumber())
        );
        // Create ESSCertIDv2
        ESSCertIDv2 essCert = new ESSCertIDv2(
            new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256),
            certHash,
            issuerSerial
        );

        // Create SigningCertificateV2
        SigningCertificateV2 scv2 = new SigningCertificateV2(new ESSCertIDv2[]{essCert});

        // Add signingCertificateV2 to signed attributes
        return(new Attribute(
            PKCSObjectIdentifiers.id_aa_signingCertificateV2,
            new DERSet(scv2)
        ));
    }
		
}
