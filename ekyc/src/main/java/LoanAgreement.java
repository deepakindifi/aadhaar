import org.json.simple.*;
import java.awt.Color;
import org.json.simple.parser.*;
import java.io.*;
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
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
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
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Document.*;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalBlankSignatureContainer;
import com.itextpdf.text.pdf.security.ExternalSignatureContainer;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import com.itextpdf.text.pdf.BaseFont;
import org.apache.commons.codec.digest.DigestUtils;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.*;

public class LoanAgreement {

	public static JSONObject decodeJson(String data) {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			JSONObject jsObj = (JSONObject) obj;
			return jsObj;
		} catch(Exception ex) {return null;}
	}

	public static String[] annotate(String payload) throws Exception {
		System.out.println(payload);
		JSONObject jsObj = LoanAgreement.decodeJson(payload);
		String rid = (String)jsObj.get("rid");
		String annotationsStr = ((String)jsObj.get("annotation"));
		String[] annotation = new String[0];
		if(annotationsStr != null && annotationsStr.length() > 1) {
			annotation = annotationsStr.split("\\|");
		}
		System.out.println(annotation.length);
		String[] all_files = ((String)jsObj.get("all_files")).split("\\|");
		for(int i = 0; i < all_files.length; i++) {
			String filename = all_files[i];
			System.out.println(filename);
			PdfReader reader;
			PdfStamper stamper;
			reader  = new PdfReader("../downloads/" + filename);
			reader.unethicalreading = true;
			stamper = new PdfStamper(reader, new FileOutputStream("../uploads/" + filename));
			BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
			int total = reader.getNumberOfPages() + 1;
			for (int l = 0; l <= 1; l++) {
				if(l == 0) {
					reader  = new PdfReader("../downloads/" + filename);
					reader.unethicalreading = true;
                                        stamper = new PdfStamper(reader, new FileOutputStream("../upload/" + filename));
				} else {
					reader  = new PdfReader("../upload/" + filename);
                                        reader.unethicalreading = true;
                                        stamper = new PdfStamper(reader, new FileOutputStream("../uploads/" + filename));
				}
				PdfContentByte under, over;
				String actualAnnotation = "";
				stamper.setRotateContents(false);
				for(int j = 1; j < total; j++) {
					over = stamper.getOverContent(j);
	                                over.beginText();
                	                if(l == 0) {
						if(!filename.equals("loanagreement-nikon.pdf") && !filename.startsWith("demand_promis") && !filename.equals("loanagreement-edelweiss.pdf") && annotation.length != 0) {
							float percentage = 0.85f;
        	                                        PdfDictionary page = reader.getPageN(j);
                	                                float offsetX = (reader.getPageSize(j).getWidth() * (1 - percentage)) / 2;
                        	                        float offsetY = (reader.getPageSize(j).getHeight() * (1 - percentage)) / 2;
                                	                stamper.getUnderContent(j).setLiteral(String.format("\nq %s 0 0 %s %s %s cm\nq\n", percentage, percentage, offsetX, offsetY));						     }
                	                } else {
						int spacing = 20;
						if(annotation.length == 1) {
							spacing = 250;
						} 
						if(annotation.length == 2) {
                                                        spacing = 150;
                                                } 
						if(annotation.length == 3) {
                                                        spacing = 50;
                                                }
						for(int k = 0; k < annotation.length; k++) {
							int space = Math.round(3 * 180f/annotation.length);
							System.out.println(annotation[k]);
							String annot = annotation[k];
							String annot1 = "";
							String[] annotPieces = annotation[k].split("for");
							if(annotPieces.length > 1) {
								annot1 = annot.substring(annot.indexOf("for") + 4);
								annot = annotPieces[0];
							}
								
							over.endText();
							over.beginText();
							over.setFontAndSize(bf, 8);
							if(annotation.length > 3) {
								over.setFontAndSize(bf, 8);
							}
		                                        over.setRGBColorFill(0,0,0);
								over.setTextMatrix(spacing+10, 30);
								over.showText(annot.substring(0,annot.indexOf(':')));
								over.setFontAndSize(bf, 8);
                                                        	over.setRGBColorFill(76,111,174);
                                                        	over.setTextMatrix(spacing+10, 20);
								if(annot1.equals("")) {
	                                                        	over.showText(annot.substring(annot.indexOf(':')+2));
								}else {
									over.showText(annot1);
									over.setTextMatrix(spacing+10, 10);
									over.showText("(" + annot.substring(annot.indexOf(':')+2) + ")");
								}
								
							spacing += space;
							
						}
					}
					over.endText();
				}
				if(l == 1 && total%2 == 0) {
					stamper.insertPage(reader.getNumberOfPages() + 1,reader.getPageSizeWithRotation(1));
					over = stamper.getOverContent(total);
					over.beginText();
					over.setFontAndSize(bf, 18);
					over.setRGBColorFill(0,0,0);
                                       	over.setTextMatrix(170, 450);
                                        over.showText("This page is intentionally left blank.");
					over.endText();
					
				}
				stamper.close();
			}
		}

		Document document = new Document();
	        FileOutputStream outputStream = new FileOutputStream("../uploads/" + rid + ".pdf");
        	PdfCopy copy = new PdfSmartCopy(document, outputStream);
	        document.open();
        	for (String fil: all_files) {
            		PdfReader reader = new PdfReader("../uploads/" + fil);
            		copy.addDocument(reader);
            		reader.close();
        	}
	        document.close();
		Process p1=Runtime.getRuntime().exec("scp ../uploads/" + rid + ".pdf" + " ubuntu@172.31.19.74:/home/ubuntu/automated_deployment/indifi_source/arya/uploads/");
                int exitValue = p1.waitFor();
		BufferedReader stdError = new BufferedReader(new 
     InputStreamReader(p1.getErrorStream()));
		System.out.println(exitValue);
		String s = null;
while ((s = stdError.readLine()) != null) {
    System.out.println(s);
}

		String[] response = {"",rid};
		return response;
	}
}
