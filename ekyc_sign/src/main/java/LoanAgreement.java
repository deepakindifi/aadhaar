import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Document.*;
import com.itextpdf.text.pdf.*;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.FileOutputStream;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;

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
			reader  = new PdfReader("/home/ubuntu/downloads/" + filename);
			reader.unethicalreading = true;
			stamper = new PdfStamper(reader, new FileOutputStream("/home/ubuntu/uploads/" + filename));
			BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
			int total = reader.getNumberOfPages() + 1;
			for (int l = 0; l <= 1; l++) {
				if(l == 0) {
					reader  = new PdfReader("/home/ubuntu/downloads/" + filename);
					reader.unethicalreading = true;
                                        stamper = new PdfStamper(reader, new FileOutputStream("/home/ubuntu/upload/" + filename));
				} else {
					reader  = new PdfReader("/home/ubuntu/upload/" + filename);
                                        reader.unethicalreading = true;
                                        stamper = new PdfStamper(reader, new FileOutputStream("/home/ubuntu/uploads/" + filename));
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
	        FileOutputStream outputStream = new FileOutputStream("/home/ubuntu/uploads/" + rid + ".pdf");
        	PdfCopy copy = new PdfSmartCopy(document, outputStream);
	        document.open();
        	for (String fil: all_files) {
            		PdfReader reader = new PdfReader("/home/ubuntu/uploads/" + fil);
            		copy.addDocument(reader);
            		reader.close();
        	}
	        document.close();
		Process p1=Runtime.getRuntime().exec("scp /home/ubuntu/uploads/" + rid + ".pdf" + " " + ReceiveRequest.RABBITMQ_ADDRESS + ":/home/ubuntu/automated_deployment/indifi_source/arya/uploads/");
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
