package com.javafxserver.digitalsigner;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import com.javafxserver.utils.TokenUtil;

//SignatureInterface implementation to handle the signing process
public class EpassSignature implements SignatureInterface{
	public final TokenDetails tokenDetails;
    public final String pin;

    public EpassSignature(TokenDetails tokenDetails, String pin) {
        this.tokenDetails = tokenDetails;
        this.pin = pin;
        System.out.println("EpassSignature initialized with token details and pin.");
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
			
			// Validate the PIN with the token
            if (!TokenUtil.isTokenPresent(pkcs11Provider, pin.toCharArray())) {
                throw new IOException("Invalid token: Token is not present.");
            }
            // Get private key and certificate from token
            PrivateKey privateKey = (PrivateKey) tokenDetails.getKeyStore().getKey(alias, pin.toCharArray());
            X509Certificate cert = (X509Certificate) tokenDetails.getKeyStore().getCertificate(alias);
            
        
            // Certificate chain as a list
            Certificate[] certChain = tokenDetails.getKeyStore().getCertificateChain(alias);
            List<Certificate> certList = Arrays.asList(certChain);
            JcaCertStore certStore = new JcaCertStore(certList);

            // Prepare the signer
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").setProvider(pkcs11Provider).build(privateKey);
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().build()
                ).build(contentSigner, cert)
            );
            generator.addCertificates(certStore);

            // Generate signed data (detached = true)
            CMSSignedData signedData = generator.generate(cmsData, false);

            // Return the encoded PKCS#7/CMS signature
            return signedData.getEncoded();
			
        
        } catch (Exception e) {
			throw new IOException("Error in retrieving private key or certificate: " + e.getMessage(), e);
		}            
    }
    
    
    
    public byte[] signWithOldMethod(InputStream content) throws IOException {
        try {
            Enumeration<String> aliases = tokenDetails.getKeyStore().aliases();
            if (!aliases.hasMoreElements()) {
                throw new IOException("No certificates found in token.");
            }
            String alias = "";
            // finding the number of aliases
            int aliasCount = 0;
            
            while (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
                System.out.println("Found alias: " + alias);
                aliasCount++;
            }
            
            System.out.println("Aliases in the token:" + aliasCount);

            PrivateKey privateKey = (PrivateKey) tokenDetails.getKeyStore().getKey(alias, pin.toCharArray());

            byte[] contentBytes = content.readAllBytes();

            Signature signature = Signature.getInstance("SHA256withRSA", tokenDetails.getPkcs11Provider());  // uses JDK provider
            signature.initSign(privateKey);  // this supports P11 key
            signature.update(contentBytes);

            return signature.sign();

        } catch (Exception e) {
            throw new IOException("Error during signing process, " + e.getMessage(), e);
        }
    }
}
