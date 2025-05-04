package com.javafxserver.digitalsigner;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.json.JSONObject;

import com.javafxserver.utils.HexDecoder;

public class CertificateInfo {
	private String alias;
    private String subjectDN;
    private String issuerDN;
    private Date validFrom;
    private Date validTo;
    private PublicKey publicKey;
    private Map<String, String> subjectAttributes = new HashMap<>();
    private Map<String, String> issuerAttributes = new HashMap<>();
    
    public static final Map<String, String> OID_TO_NAME_MAP = new HashMap<>();
    public static final Map<String, String> NAME_TO_FULLFORM_MAP = new HashMap<>();

    static {
    	OID_TO_NAME_MAP.put("2.5.4.65", "PSEUDONYM");
        OID_TO_NAME_MAP.put("2.5.4.3", "CN"); // Common Name
        OID_TO_NAME_MAP.put("2.5.4.51", "HOUSEIDENTIFIER"); // Common Name
        OID_TO_NAME_MAP.put("2.5.4.20", "TELEPHONENUMBER"); // Telephone Number
        OID_TO_NAME_MAP.put("2.5.4.6", "C");  // Country
        OID_TO_NAME_MAP.put("2.5.4.7", "L");  // Locality
        OID_TO_NAME_MAP.put("2.5.4.8", "ST"); // State or Province
        OID_TO_NAME_MAP.put("2.5.4.10", "O"); // Organization
        OID_TO_NAME_MAP.put("2.5.4.11", "OU"); // Organizational Unit
        OID_TO_NAME_MAP.put("2.5.4.9", "STREET"); // Street Address
        OID_TO_NAME_MAP.put("2.5.4.17", "POSTALCODE"); // Postal Code
        OID_TO_NAME_MAP.put("2.5.4.12", "TITLE"); // Title
        OID_TO_NAME_MAP.put("2.5.4.4", "SN"); // Surname
        OID_TO_NAME_MAP.put("2.5.4.42", "GIVENNAME"); // Given Name
        OID_TO_NAME_MAP.put("2.5.4.43", "INITIALS"); // Initials
        OID_TO_NAME_MAP.put("2.5.4.44", "GENERATION"); // Generation Qualifier
        OID_TO_NAME_MAP.put("2.5.4.5", "SERIALNUMBER"); // Serial Number
        OID_TO_NAME_MAP.put("0.9.2342.19200300.100.1.1", "UID"); // User ID
        OID_TO_NAME_MAP.put("0.9.2342.19200300.100.1.25", "DC"); // Domain Component
        OID_TO_NAME_MAP.put("1.2.840.113549.1.9.1", "EMAILADDRESS"); // Email Address
    }
    
    static {
		NAME_TO_FULLFORM_MAP.put("CN", "Common Name");
		NAME_TO_FULLFORM_MAP.put("PSEUDONYM", "Pseudonym");
		NAME_TO_FULLFORM_MAP.put("HOUSEIDENTIFIER", "House Identifier");
		NAME_TO_FULLFORM_MAP.put("TELEPHONENUMBER", "Telephone Number");
		NAME_TO_FULLFORM_MAP.put("C", "Country");
		NAME_TO_FULLFORM_MAP.put("L", "Locality");
		NAME_TO_FULLFORM_MAP.put("ST", "State or Province");
		NAME_TO_FULLFORM_MAP.put("O", "Organization");
		NAME_TO_FULLFORM_MAP.put("OU", "Organizational Unit");
		NAME_TO_FULLFORM_MAP.put("STREET", "Street Address");
		NAME_TO_FULLFORM_MAP.put("POSTALCODE", "Postal Code");
		NAME_TO_FULLFORM_MAP.put("TITLE", "Title");
		NAME_TO_FULLFORM_MAP.put("SN", "Surname");
		NAME_TO_FULLFORM_MAP.put("GIVENNAME", "Given Name");
		NAME_TO_FULLFORM_MAP.put("INITIALS", "Initials");
		NAME_TO_FULLFORM_MAP.put("GENERATION", "Generation Qualifier");
		NAME_TO_FULLFORM_MAP.put("SERIALNUMBER", "Serial Number");
		NAME_TO_FULLFORM_MAP.put("UID", "User ID");
		NAME_TO_FULLFORM_MAP.put("DC", "Domain Component");
		NAME_TO_FULLFORM_MAP.put("EMAILADDRESS", "Email Address");
	}


    // Getters and setters...

    @Override
    public String toString() {
        return "Alias: " + alias + "\n" +
               "Subject DN: " + subjectDN + "\n" +
               "Issuer DN: " + issuerDN + "\n" +
               "Valid From: " + validFrom + "\n" +
               "Valid To: " + validTo + "\n";
    }
    
    //public key
    public void setPublicKey(PublicKey publicKey) {
    	this.publicKey = publicKey;
    }
    
    public PublicKey getPublicKey() {
    	return this.publicKey;
    }
    
    public JSONObject toJson() {
    	JSONObject certInfo = new JSONObject();
    	certInfo.put("alias", this.alias);
    	certInfo.put("valid_from", this.validFrom);
    	certInfo.put("valid_to", this.validTo);
    	certInfo.put("subject_detail", getSubjectDetailsInJSON());
    	certInfo.put("issuer_detail", getIssuerDetailsInJSON());
    	//Based 64 encoded public key
    	String based64publicKey = Base64.getEncoder().encodeToString(this.getPublicKey().getEncoded());
    	certInfo.put("encoded_public_key", "-----BEGIN PUBLIC KEY-----\n"+based64publicKey+"\n-----END PUBLIC KEY-----");
    	return certInfo;
    }

	public void setAlias(String alias2) {
		// TODO Auto-generated method stub
		this.alias = alias2;
	}
	
	public void setSubjectDN(String subjectDN2) {
		// TODO Auto-generated method stub
		this.subjectDN = subjectDN2;
	}
	
	public void setIssuerDN(String issuerDN2) {
		// TODO Auto-generated method stub
		this.issuerDN = issuerDN2;
	}
	
	public void setValidFrom(Date valid_from) {
		// TODO Auto-generated method stub
		this.validFrom = valid_from;
	}
	
	public void setValidTo(Date valid_to) {
		// TODO Auto-generated method stub
		this.validTo = valid_to;
	}
	
	private Map<String, String> getAttributes(String dn) throws Exception {
		LdapName ldapName = new LdapName(dn);
		Map<String, String> attributes = new HashMap<>();

		for (Rdn rdn : ldapName.getRdns()) {
			String type = rdn.getType();
			Object value = rdn.getValue();
			
			// Map OID to attribute name if applicable
	        String attributeName = OID_TO_NAME_MAP.getOrDefault(type, type);
	        String decodedValue = null;
			// Check if the value is a byte array (encoded in hex)
			if (value instanceof byte[]) {
				byte[] bytes = (byte[]) value;
				decodedValue = new String(bytes, "UTF-8");
			} else {
				String valueStr = value.toString();
	            // Check if the value starts with a '#' indicating hexadecimal encoding
	            if (valueStr.startsWith("#")) {
	                String hexValue = valueStr.substring(1); // Remove the '#'
	                decodedValue = HexDecoder.decodeHexToString(hexValue);
	            } else {
	                decodedValue = valueStr;
	            }
			}
			attributes.put(attributeName, decodedValue);
		}

		return attributes;
	}
	
	
	public void parseSubjectDN() throws Exception {
		subjectAttributes = getAttributes(this.subjectDN);
    }
	
	public void parseIssuerDN() throws Exception {
		issuerAttributes = getAttributes(this.issuerDN);
	}
	
	public Map<String, String> getSubjectAttributes() {
		return subjectAttributes;
	}
	
	public Map<String, String> getIssuerAttributes() {
		return issuerAttributes;
	}
	
	/***** Return String ****/
	public String getSubjectDetails() {		
		return getAttributeDetails(subjectAttributes);
	}
	
	public String getIssuerDetails() {		
		return getAttributeDetails(issuerAttributes);
	}
	
	private String getAttributeDetails(Map<String, String> attributes) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : attributes.entrySet()) {			
			//Getting full name of the key
			String fullNameKey = NAME_TO_FULLFORM_MAP.get(entry.getKey());			
			sb.append(fullNameKey).append(": ").append(entry.getValue()).append("\n");
		}
		return sb.toString();
	}
	
	public String getSubjectDetails(String key) {
		return subjectAttributes.get(key);
	}
	
	public String getIssuerDetails(String key) {
		return issuerAttributes.get(key);
	}
	
	public String getValidFrom() {
		return validFrom.toString();
	}
	
	public String getValidTill() {
		return validTo.toString();
	}
	
	
	/**** return JSONObject *****/	
	private JSONObject getAttributeDetailsInJSON(Map<String, String> attributes) {
		JSONObject object = new JSONObject();
		for (Map.Entry<String, String> entry : attributes.entrySet()) {			
			//Getting full name of the key
			String fullNameKey = NAME_TO_FULLFORM_MAP.get(entry.getKey()).replace(" ", "_");			
			object.put(fullNameKey, removeControlChars((String) entry.getValue()));
		}
		return object;
	}
	
	public JSONObject getSubjectDetailsInJSON() {
		return getAttributeDetailsInJSON(subjectAttributes);
	}
	
	public JSONObject getIssuerDetailsInJSON() {
		return getAttributeDetailsInJSON(issuerAttributes);
	}
	
	public static String removeControlChars(String input) {
	    return input.replaceAll("\\p{Cntrl}", "").trim();
	}
	
}
