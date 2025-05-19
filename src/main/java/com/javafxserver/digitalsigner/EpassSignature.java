package com.javafxserver.digitalsigner;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TimeStampToken;

import com.javafxserver.exceptions.DigitalSigningException;
import com.javafxserver.exceptions.NoCertificateChainException;
import com.javafxserver.exceptions.NoCertificateFoundException;
import com.javafxserver.service.TokenService;

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
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;

public class EpassSignature implements SignatureInterface {
    public final TokenService tokenService;
    public final String pin;

    public EpassSignature(TokenService tokenService, String pin) {
        this.tokenService = tokenService;
        this.pin = pin;
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            byte[] contentBytes = content.readAllBytes();
            Provider pkcs11Provider = tokenService.getPkcs11Provider();

            CMSProcessableByteArray cmsData = new CMSProcessableByteArray(contentBytes);
            
            String alias = tokenService.getFirstCertificateAlias();
            if (alias == null) {
                throw new NoCertificateFoundException("No private key entry found in the token.", 
                		new IOException("The KeyStore has no alias"));
            }

            PrivateKey privateKey = (PrivateKey) tokenService.getKeyStore().getKey(alias, pin.toCharArray());
            X509Certificate userCert = (X509Certificate) tokenService.getKeyStore().getCertificate(alias);

            //Certificate[] certChain = tokenService.getKeyStore().getCertificateChain(alias);
            List<X509Certificate> fullChain = tokenService.getX509CertificateChain();

            System.out.println("Certificate Chain:");
            for(X509Certificate cert : fullChain) {
            	System.out.println("Certificate: " + cert.getSubjectX500Principal());
            }
            
            if (fullChain == null || fullChain.isEmpty()) {
                throw new NoCertificateChainException("No certificate chain found in token.");
            }
            
            // The last certificate in the chain should be the root CA.
            // Check if the last certificate is self-signed:
            X509Certificate lastCert = fullChain.get(fullChain.size()-1);//user certificate
            
            if (!lastCert.getSubjectX500Principal().equals(lastCert.getIssuerX500Principal())) {
				System.out.println("The last certificate in the chain is not self-signed.");
			}
            
            if(userCert.equals(lastCert)) {
				System.out.println("User certificate is the last certificate in the chain.");
			}
        	
            JcaCertStore certStore = new JcaCertStore(fullChain);

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(pkcs11Provider)
                    .build(privateKey);

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().build()
                    )
                            .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(getSignedAttribute(userCert)))
                            .build(contentSigner, userCert)
            );
            generator.addCertificates(certStore);

            CMSSignedData signedData = generator.generate(cmsData, false);

            // Add timestamp token (TSA)
            //TimeStampToken tsToken = TSAClient.getTimeStampToken(signedData.getEncoded());
            //signedData = TimeStampDataConvertor.addTimestampToSignature(signedData, tsToken);

            return signedData.getEncoded();
        } catch (Exception e) {
            throw new DigitalSigningException(e.getMessage(), e);
        }
    }

    private AttributeTable getSignedAttribute(X509Certificate cert) throws Exception {
        Hashtable<ASN1ObjectIdentifier, Attribute> attrMap = new Hashtable<>();

        Attribute signingTimeAttr = new Attribute(
                PKCSObjectIdentifiers.pkcs_9_at_signingTime,
                new DERSet(new Time(new Date()))
        );

        Attribute contentTypeAttr = new Attribute(
                PKCSObjectIdentifiers.pkcs_9_at_contentType,
                new DERSet(PKCSObjectIdentifiers.data)
        );

        Attribute signingCertificateV2 = getSigningCertificateV2(cert);

        attrMap.put(contentTypeAttr.getAttrType(), contentTypeAttr);
        attrMap.put(signingTimeAttr.getAttrType(), signingTimeAttr);
        attrMap.put(signingCertificateV2.getAttrType(), signingCertificateV2);

        return new AttributeTable(attrMap);
    }

    public Attribute getSigningCertificateV2(X509Certificate cert) throws Exception {
        byte[] certHash = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());

        X500Principal issuerPrincipal = cert.getIssuerX500Principal();
        X500Name issuerName = new X500Name(issuerPrincipal.getName());

        GeneralName issuerGeneralName = new GeneralName(GeneralName.directoryName, issuerName);
        IssuerSerial issuerSerial = new IssuerSerial(new GeneralNames(issuerGeneralName), new ASN1Integer(cert.getSerialNumber()));

        ESSCertIDv2 essCert = new ESSCertIDv2(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256), certHash, issuerSerial);
        SigningCertificateV2 scv2 = new SigningCertificateV2(new ESSCertIDv2[]{essCert});

        return new Attribute(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new DERSet(scv2));
    }

}
