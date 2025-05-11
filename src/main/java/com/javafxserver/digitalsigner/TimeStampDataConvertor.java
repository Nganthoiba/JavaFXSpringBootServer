package com.javafxserver.digitalsigner;

import java.util.Collections;
import java.util.List;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.tsp.TimeStampToken;

public class TimeStampDataConvertor {
	public static CMSSignedData convertToTimeStampData(CMSSignedData signedData) throws Exception {
    	// Call TSA to get timestamp
    	TimeStampToken tsToken = TSAClient.getTimeStampToken(signedData.getEncoded());

    	// Add the timestamp as an unsigned attribute to the signer info
    	SignerInformation signer = signedData.getSignerInfos().getSigners().iterator().next();

    	AttributeTable unsignedAttributes = new AttributeTable(
    	    new DERSet(
    	        new Attribute(
    	            PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
    	            new DERSet(ASN1Primitive.fromByteArray(tsToken.getEncoded()))
    	        )
    	    )
    	);

    	SignerInformation tsSigner = SignerInformation.replaceUnsignedAttributes(signer, unsignedAttributes);

    	// Replace the old signer info with the timestamped one
    	SignerInformationStore newSignerStore = new SignerInformationStore(List.of(tsSigner));

    	CMSSignedData timestampedData = CMSSignedData.replaceSigners(signedData, newSignerStore);
    	return timestampedData;
    }
	
	public static CMSSignedData addTimestampToSignature(CMSSignedData signedData, TimeStampToken tsToken) throws Exception {
	    SignerInformation signer = signedData.getSignerInfos().getSigners().iterator().next();
	    
	    // Create unsigned attributes with timestamp token
	    AttributeTable unsignedAttributes = new AttributeTable(new DERSet(new Attribute(
	            PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
	            new DERSet(new DEROctetString(tsToken.getEncoded())) 
	    )));

	    // Replace unsigned attributes in signer information
	    SignerInformation tsSigner = SignerInformation.replaceUnsignedAttributes(signer, unsignedAttributes);
	    
	    return CMSSignedData.replaceSigners(signedData, new SignerInformationStore(Collections.singletonList(tsSigner)));
	}

}

/*
try {
	CMSSignedData timestampedData = convertToTimeStampData(signedData);
	// Return the encoded PKCS#7/CMS signature
	return timestampedData.getEncoded();
} catch (Exception e) {
	//throw new IOException("Error in adding timestamp: " + e.getMessage(), e);
	return signedData.getEncoded();
}
*/
