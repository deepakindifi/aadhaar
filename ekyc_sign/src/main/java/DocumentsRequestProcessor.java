import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Document.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import org.apache.commons.codec.net.*;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.FileOutputStream;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class DocumentsRequestProcessor implements RequestProcessor {

	public static JSONObject decodeJson(String data) {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			JSONObject jsObj = (JSONObject) obj;
			return jsObj;
		} catch(Exception ex) {return null;}
	}

	public JSONObject processRequest(String payload) {
		System.out.println(payload);
		JSONObject jsObj = PdfGenerator.decodeJson(payload);
		String[] imageFormats = {"jpg","jpeg","png","tif","bmp","gif","svg","tiff"};
		String borrower_name = ((String)jsObj.get("business_name"));
		JSONObject doc = (JSONObject)jsObj.get("document");
		String filename = (String)doc.get("filename");
		String url = (String)doc.get("pdf_url");
		String documentId = (String)doc.get("id");
        	String format = filename.substring(filename.lastIndexOf(".")+1);
        	String newFileName = new StringBuilder(filename.replace(format, "pdf")).insert(filename.lastIndexOf("."), "-generated").toString(); 
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
		filename = filename.replace(format,"pdf");
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
        JSONObject response = new JSONObject();
        response.put("message", jsonObject.toString());
        response.put("topic", "process_generated_pdf");
        return response;
	}
}
