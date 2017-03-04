import org.json.simple.*;
import org.json.simple.parser.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.net.HttpURLConnection;
import javax.net.ssl.*;

import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.codec.binary.Base64;
import javax.xml.crypto.dsig.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.dom.*;
import javax.xml.crypto.dsig.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;




public final class EKycRequestProcessor implements RequestProcessor {
	private static final String JCE_PROVIDER = "BC";
	private static final String algorithm = "SHA-256";
	private static final String SECURITY_PROVIDER = "BC";

	private static final String ASYMMETRIC_ALGO = "RSA/ECB/PKCS1Padding";
	private static final int SYMMETRIC_KEY_SIZE = 256;

	private static final String CERTIFICATE_TYPE = "X.509";

	private static PublicKey publicKey;
	private static Date certExpiryDate;

	public static JSONObject decodeJson(String data) {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			JSONObject jsObj = (JSONObject) obj;
			return jsObj;
		} catch(Exception ex) {return null;}
	}

	public static byte[] encryptWithSessionKey(byte[] symmKey, byte[] data) {
		try{
			PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new AESEngine(), new PKCS7Padding());
			cipher.init(true, new KeyParameter(symmKey));
			int outputSize = cipher.getOutputSize(data.length);
			byte[] tempOP = new byte[outputSize];

			int processLen = cipher.processBytes(data, 0, data.length, tempOP, 0);
			int outputLen = cipher.doFinal(tempOP, processLen);
			byte[] result = new byte[processLen + outputLen];
			System.arraycopy(tempOP, 0, result, 0, result.length);
			return result;
		} catch(Exception Ex) {return null;}
	}

	public static byte[] hashPayload(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm, SECURITY_PROVIDER);
			digest.reset();
			byte[] hash = digest.digest(data);
			return hash;
		} catch(Exception ex) {return null;}
	}

	public static void signXml() {
		try {
			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
			Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null),Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),null, null);
			SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,(C14NMethodParameterSpec) null),fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),Collections.singletonList(ref));
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("src/main/resources/indifi"), "Alok@1nd1f1".toCharArray());
			KeyStore.PrivateKeyEntry keyEntry =(KeyStore.PrivateKeyEntry) ks.getEntry("le-dd665bdc-a4ab-423a-9ecc-f777be44ea13", new KeyStore.PasswordProtection("emudhra".toCharArray()));
			
			X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
			KeyInfoFactory kif = fac.getKeyInfoFactory();
			List x509Content = new ArrayList();
			x509Content.add(cert.getSubjectX500Principal().getName());
			x509Content.add(cert);
			X509Data xd = kif.newX509Data(x509Content);
			KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document doc = dbf.newDocumentBuilder().parse(new FileInputStream("purchaseOrder3.xml"));

			DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());
			XMLSignature signature = fac.newXMLSignature(si, ki);
			signature.sign(dsc);
			Node node = dsc.getParent();
			Document signedDocument = node.getOwnerDocument();
			StringWriter stringWriter = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer trans = tf.newTransformer();
			trans.transform(new DOMSource(signedDocument), new StreamResult(stringWriter));
		} catch(Exception ex) {}
	}

	public static String sendHttpRequest(String urlParameters, String url) {
		/*
		//String url = "https://server2.e-mudhra.com:8443/eServices/v1_0/signdoc";
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		con.setHostnameVerifier(new HostnameVerifier() {     
	    	public boolean verify(String hostname, SSLSession session) {
        		return true;
			}
		});
        con.setRequestMethod("POST");

        String urlParameters = stringWriter.getBuffer().toString();

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(URLEncoder.encode(urlParameters));
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
		return response.toString();	
		*/
		return "";
	}

	public JSONObject processRequest (String pl) {
		JSONObject response = new JSONObject();
		try {
			Security.addProvider(new BouncyCastleProvider());
			JSONObject jsObj = EKycRequestProcessor.decodeJson(pl);
			String data = (String)jsObj.get("pid");
			String payload = (String)jsObj.get("xml");
		//String payload = "%skey%\n%hmac%\n%pid%";
			CertificateFactory certFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE, JCE_PROVIDER);
			FileInputStream fileInputStream = new FileInputStream(new File("src/main/resources/session.pem"));
			X509Certificate cert = (X509Certificate) certFactory.generateCertificate(fileInputStream);
			PublicKey publicKey = cert.getPublicKey();
			Date certExpiryDate = cert.getNotAfter();
			KeyGenerator kgen = KeyGenerator.getInstance("AES", JCE_PROVIDER);
			kgen.init(SYMMETRIC_KEY_SIZE);
			SecretKey key = kgen.generateKey();
			byte[] symmKey = key.getEncoded();
			Cipher pkCipher = Cipher.getInstance(ASYMMETRIC_ALGO, JCE_PROVIDER);
			pkCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encSessionKey = pkCipher.doFinal(symmKey);
			byte[] finalDataBlock = EKycRequestProcessor.encryptWithSessionKey(symmKey, data.getBytes());
			byte[] hashedData = EKycRequestProcessor.hashPayload(data.getBytes());
			byte[] hmac = EKycRequestProcessor.encryptWithSessionKey(symmKey, hashedData);   
			payload = payload.replace("%pid%",new String(Base64.encodeBase64(finalDataBlock)));
			payload = payload.replace("%hmac%",new String(Base64.encodeBase64(hmac)));
			payload = payload.replace("%skey%",new String(Base64.encodeBase64(encSessionKey)));
			response.put("message", payload);
			response.put("topic", (String)jsObj.get("id"));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return response;
	}
}
