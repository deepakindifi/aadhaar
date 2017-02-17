import org.json.simple.*;
import java.awt.Color;
import org.json.simple.parser.*;
import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import javax.net.ssl.*;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Calendar;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.itextpdf.text.Document.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import org.apache.commons.codec.digest.DigestUtils;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class PdfGenerator {

	public static JSONObject decodeJson(String data) {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			JSONObject jsObj = (JSONObject) obj;
			return jsObj;
		} catch(Exception ex) {return null;}
	}

	public static JSONObject generatePdf(String payload) {
		System.out.println(payload);
		JSONObject jsObj = PdfGenerator.decodeJson(payload);
		String[] imageFormats = {"jpg","jpeg","png","tif","bmp","gif","svg","tiff"};
		String borrower_name = ((String)jsObj.get("business_name"));
		JSONObject doc = (JSONObject)jsObj.get("document");
		String filename = (String)doc.get("filename");
		String url = (String)doc.get("pdf_url");
		String documentId = (String)doc.get("id");
        	String format = filename.substring(filename.lastIndexOf(".")+1);
        	String newFileName = new StringBuilder(filename.replace(format, "pdf")).insert(filename.lastIndexOf("."), "-attested").toString(); 
		try {
		AmazonS3Util.downloadFile(filename, url);
		System.out.println(filename);
		if(Arrays.asList(imageFormats).contains(format)) {
			Document document = new Document();
			PdfWriter.getInstance(document, new FileOutputStream("src/main/resources/downloads/" + filename.replace(format,"pdf")));
			document.open();
			Image img = Image.getInstance("src/main/resources/downloads/" + filename);
			float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / img.getWidth()) * 100;
			img.scalePercent(scaler);
			document.add(img);
			document.close();
		}
		System.out.println("downloaded");
		Image sign = Image.getInstance("src/main/resources/signature.png");
		sign.scalePercent(70);
			for(int k = 0; k <= 1; k++) {
				PdfReader reader;
				PdfStamper stamper;
				if(k == 0) {
					System.out.println("downloaded here");
					reader  = new PdfReader("src/main/resources/downloads/" + filename);
					reader.unethicalreading = true;
					stamper = new PdfStamper(reader, new FileOutputStream("src/main/resources/temp/" + filename));
				} else {
					reader  = new PdfReader("src/main/resources/temp/" + filename);
					reader.unethicalreading = true;
					stamper = new PdfStamper(reader, new FileOutputStream("src/main/resources/uploads/" + newFileName));
				} 
				BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
				PdfContentByte under, over;
				int total = reader.getNumberOfPages() + 1;
				PdfDictionary page;
				PdfArray crop;
				PdfArray media;
				String actualAnnotation = "";
				for (int j = 1; j < total; j++) {
					stamper.setRotateContents(false);
					if(k == 1) {
						over = stamper.getOverContent(j);
						over.beginText();
						over.setFontAndSize(bf, 10);
						over.setRGBColorFill(0, 0, 0);
						sign.setAbsolutePosition(200, 40);
						over.addImage(sign);
						over.setTextMatrix(210, 30);
						over.showText("Authorized Signatory for " + borrower_name);
						over.endText();
					} else {
						float percentage = 0.75f;
						page = reader.getPageN(j);
						float offsetX = (reader.getPageSize(j).getWidth() * (1 - percentage)) / 2;
						float offsetY = (reader.getPageSize(j).getHeight() * (1 - percentage)) / 2;
						stamper.getUnderContent(j).setLiteral(String.format("\nq %s 0 0 %s %s %s cm\nq\n", percentage, percentage, offsetX, offsetY));
					}
				}
				stamper.close();
			}
		} catch(Exception ex) {
			System.out.println("Error Encountered " + ex.getMessage());
			ex.printStackTrace();
			JSONObject jsonObject = new JSONObject();
        	jsonObject.put("id", documentId);
        	jsonObject.put("error_msg", ex.getMessage());
        	return jsonObject;
		}
		String httpUrl = AmazonS3Util.uploadFile(newFileName, url);
		JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", documentId);
        doc.put("generated_url", httpUrl);
        jsonObject.put("document", doc);
        return jsonObject;
	}
}
