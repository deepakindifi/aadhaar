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
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import org.apache.commons.codec.digest.DigestUtils;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.*;

public class Annotator {

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
		JSONObject jsObj = Annotator.decodeJson(payload);
		String rid = (String)jsObj.get("rid");
		String[] annotation = ((String)jsObj.get("annotation")).split("\\|");
		String[] image_files = ((String)jsObj.get("image_files")).split("\\|");
		String[] all_files = ((String)jsObj.get("all_files")).split("\\|");
		String[] file_types = ((String)jsObj.get("file_types")).split("\\|");
		String[] all_entities = ((String)jsObj.get("all_entities")).split("\\|");
		String pending_documents = ((String)jsObj.get("pending_documents"));
		String borrower_name = ((String)jsObj.get("borrower_name"));
		String borrower_annotation = ((String)jsObj.get("borrower_annotation"));
		String html = "<br/><br/><br/><div align='center'><b> Action Items</b> </div><br/><br/><div><table align='center' style='font-size:10pt;' border='1' cellspacing='0' cellpadding='5'> <tr font-size='8pt'><th>Page</th><th>Document Name</th><th>Submitted For</th><th>Attested By(Sign at the Bottom)</th></tr>";
		String pending_documents_html = "<br/><br/><div align='center'><b>Pending Documents</b> </div><br/><br/><div><table align='center' border='1' style='font-size:10pt;' cellspacing='0' cellpadding='5'> <tr><th>S.No.</th><th>Document Name</th><th> To Be Submitted For</th><th>To Be Attested By(Sign at the Bottom)</th></tr>";
		int i;
		for(i = 0; i < image_files.length; i++) {
			if(image_files[i].length() == 0) {
				continue;
			}
			Document document = new Document();
			String format = image_files[i].substring(image_files[i].lastIndexOf(".")+1);
			PdfWriter.getInstance(document, new FileOutputStream("/home/ubuntu/downloads/" + image_files[i].replace(format,"pdf")));
			document.open();
			Image img = Image.getInstance("/home/ubuntu/downloads/" + image_files[i]);
			float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / img.getWidth()) * 100;
			img.scalePercent(scaler);
			document.add(img);
			document.close();
		}
		int pageCount = 2;
		Image sign = Image.getInstance("src/main/resources/signature.png");
		sign.scalePercent(70);
		for(i = 0; i < all_files.length; i++) {
			try {
			String format = all_files[i].substring(all_files[i].lastIndexOf(".")+1);
			String filename = all_files[i];
			String fileType = file_types[i];
			String entityName = all_entities[i];
			System.out.println("format is " + format);
			System.out.println("file is " + filename);
			if(!format.toLowerCase().equals("pdf")) {
				filename = filename.replace(format,"pdf");
				System.out.println("final file is " + filename);
			}
			if(filename.length() == 3 || format.length() == 0) {
				continue;
			}
			for(int k = 0; k <= 1; k++) {
				PdfReader reader;
				PdfStamper stamper;
				if(k == 0) {
					reader  = new PdfReader("/home/ubuntu/downloads/" + filename);
					reader.unethicalreading = true;
					stamper = new PdfStamper(reader, new FileOutputStream("/home/ubuntu/upload/" + filename));
				} else {
					reader  = new PdfReader("/home/ubuntu/upload/" + filename);
					reader.unethicalreading = true;
					stamper = new PdfStamper(reader, new FileOutputStream("/home/ubuntu/uploads/" + filename));
				} 
				BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
				PdfContentByte under, over;
				int total = reader.getNumberOfPages() + 1;
				PdfDictionary page;
				PdfArray crop;
				PdfArray media;
				int fromPage = pageCount;
				if(i > 0) {
					fromPage = pageCount + 1;
				}
				String actualAnnotation = "";
				for (int j = 1; j < total; j++) {
					stamper.setRotateContents(false);
					if(k == 1) {
						over = stamper.getOverContent(j);
						over.beginText();
						over.setFontAndSize(bf, 10);
						over.setRGBColorFill(0, 0, 0);
						fileType = fileType.trim();
						if(!fileType.equals("Bank Statement") || (j == 1) || (j == total -1)) {
							String[] annotationPieces = annotation[i].split("\n");
							//over.setTextMatrix(30, 40);
							//over.showText(annotationPieces[0]);
							//over.setTextMatrix(30, 30);
							actualAnnotation = annotationPieces[1];
							sign.setAbsolutePosition(200, 40);
						        over.addImage(sign);
							over.setTextMatrix(210, 30);
							//ColumnText.showTextAligned(over, Element.ALIGN_CENTER, new Phrase(annotationPieces[1]), 200, 30, 0);
							over.showText(annotationPieces[1]);
						}
						over.endText();
						if(j == 1) {
							ColumnText.showTextAligned(over, Element.ALIGN_CENTER, new Phrase(String.format("%s",fileType)), 300, 800, 0);
						}
						over.setFontAndSize(bf, 6);
						ColumnText.showTextAligned(over, Element.ALIGN_RIGHT, new Phrase(String.format("Page %s", pageCount)), 559, 30, 0);
						pageCount++;
					} else {
						float percentage = 0.75f;
						page = reader.getPageN(j);
						float offsetX = (reader.getPageSize(j).getWidth() * (1 - percentage)) / 2;
						float offsetY = (reader.getPageSize(j).getHeight() * (1 - percentage)) / 2;
						stamper.getUnderContent(j).setLiteral(String.format("\nq %s 0 0 %s %s %s cm\nq\n", percentage, percentage, offsetX, offsetY));
					}
				}
				if(k == 1) {
					stamper.insertPage(reader.getNumberOfPages() + 1,reader.getPageSizeWithRotation(1));
					over = stamper.getOverContent(total);
					over.beginText();
					over.setFontAndSize(bf, 6);
					over.setRGBColorFill(0, 0, 0);	
					ColumnText.showTextAligned(over, Element.ALIGN_RIGHT, new Phrase(String.format("Page %s", pageCount)), 559, 30, 0);
					pageCount++;
					int toPage = pageCount - 1;
					String pageNo = fromPage + "-" + toPage;
					if(fromPage == toPage) {
						pageNo = String.valueOf(fromPage);
					}
					html = html + "<tr><td>" + pageNo + "</td><td>" + fileType + "</td><td>" + entityName + "</td><td>" + actualAnnotation + "</td></tr>"; 
				}
				stamper.close();
			}
			Process p=Runtime.getRuntime().exec("scp /home/ubuntu/uploads/" + filename + " ubuntu@10.0.3.68:/home/ubuntu/automated_deployment/indifi_source/arya/uploads/"); 
			//Process p=Runtime.getRuntime().exec("cp /home/ubuntu/uploads/" + filename + "/Users/agarwal/arya/core/lib/uploads/");
			p.waitFor();
			} catch(Exception ex) {
				all_files[i] = null;
			}
		}

		html = html + "</table></div>";
		if(pending_documents.length() != 0) {
			html = html + pending_documents_html + "<tr><td>1</td><td>Copy of Last 6 Months Bank Statement on Bank Letter Head</td><td>" + borrower_name + "</td><td>"  + borrower_annotation + "</td></tr></table></div>";
		}
		PrintWriter out = new PrintWriter("/home/ubuntu/uploads/" + rid + ".html");
		out.println(html);
		out.close();
		Process p=Runtime.getRuntime().exec("scp /home/ubuntu/uploads/" + rid + ".html" + " ubuntu@10.0.3.68:/home/ubuntu/automated_deployment/indifi_source/arya/uploads/");
		p.waitFor();
		Document document = new Document();
        FileOutputStream outputStream = new FileOutputStream("/home/ubuntu/uploads/" + rid + ".pdf");
        PdfCopy copy = new PdfSmartCopy(document, outputStream);
        document.open();
        for (String fil: all_files) {
	    if(fil == null) {
		continue;
	    }
	    System.out.println(fil);
	    String[] fp = fil.split("\\.");
	    System.out.println(fp[0]);
	    String ext = fp[fp.length-1];
	    System.out.println(ext);
	    String finalExt = ext;
	    if(!ext.toLowerCase().equals("pdf")) {
		finalExt = "pdf";
	    }
            System.out.println(finalExt);
            PdfReader reader = new PdfReader("/home/ubuntu/uploads/" + fil.replaceAll(ext,finalExt));
            copy.addDocument(reader);
            reader.close();
        }
        document.close();
	Process p1=Runtime.getRuntime().exec("scp /home/ubuntu/uploads/" + rid + ".pdf" + " ubuntu@10.0.3.68:/home/ubuntu/automated_deployment/indifi_source/arya/uploads/");
                p1.waitFor();
		String[] response = {"",rid};
		return response;
	}
}
