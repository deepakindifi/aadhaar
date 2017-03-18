import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;






//import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class AnnotateDocumentsProcessor implements RequestProcessor{

    private static String downloadPath = "src/main/resources/downloads/";
    private static String tempPath = "src/main/resources/temp/";
    private static String uploadPath = "src/main/resources/uploads/";

	public static JSONObject decodeJson(String data) {
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			JSONObject jsObj = (JSONObject) obj;
			return jsObj;
		} catch(Exception ex) {return null;}
	}

	public static void annotateFile(JSONObject file) throws Exception {
        String filename = (String)file.get("file_name");
        JSONArray annotationsJSArray = (JSONArray) file.get("annotations");
        
        String[] annotations = new String[annotationsJSArray.size()];
        for(int i=0;i<annotationsJSArray.size();i++) {
            annotations[i]= (String)annotationsJSArray.get(i);
        }
        String fileUrl = (String)file.get("file_url");
        String password = null;
        if(file.containsKey("file_password")) {
            password = (String)file.get("file_password");
        }
        AmazonS3Util.downloadFile(filename, fileUrl);
        filename = convertToPdfIfImage(filename);
        file.put("file_name", filename);
        CustomReader reader;
        PdfStamper stamper;
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        
        if(password == null) {
             reader  = new CustomReader(downloadPath + filename);
        } else {
            reader = new CustomReader(downloadPath + filename, password.getBytes());
        }
        reader.unethicalreading = true;
        reader.decryptOnPurpose();
        stamper = new PdfStamper(reader, new FileOutputStream(tempPath + filename));
        int total = reader.getNumberOfPages() + 1;
        for(int l = 0; l<=1; l++) {
            if(l == 1) {
                reader = new CustomReader(tempPath + filename);
                reader.unethicalreading = true;
                stamper = new PdfStamper(reader, new FileOutputStream(uploadPath + filename));
            }
            PdfContentByte under, over;
            String actualAnnotation = "";
            stamper.setRotateContents(false);
            for(int j = 1; j < total; j++) {
                over = stamper.getOverContent(j);
                over.beginText();
                if(l == 0) {
                    if(!filename.contains("loanagreement-nikon.pdf") && !filename.contains("demand_promis") && !filename.contains("loanagreement-edelweiss.pdf") && annotations.length != 0) {
                        float percentage = 0.85f;
                        PdfDictionary page = reader.getPageN(j);
                        float offsetX = (reader.getPageSize(j).getWidth() * (1 - percentage)) / 2;
                        float offsetY = (reader.getPageSize(j).getHeight() * (1 - percentage)) / 2;
                        stamper.getUnderContent(j).setLiteral(String.format("\nq %s 0 0 %s %s %s cm\nq\n", percentage, percentage, offsetX, offsetY));						     }
                } else {
                    int spacing = 20;
                    if(annotations.length == 1) {
                        spacing = 250;
                    }
                    if(annotations.length == 2) {
                        spacing = 150;
                    }
                    if(annotations.length == 3) {
                        spacing = 50;
                    }
                    for(int k = 0; k < annotations.length; k++) {
                        int space = Math.round(3 * 180f/annotations.length);
                        String annot = annotations[k];
                        String annot1 = "";
                        String[] annotPieces = annotations[k].split("for");
                        if(annotPieces.length > 1) {
                            annot1 = annot.substring(annot.indexOf("for") + 4);
                            annot = annotPieces[0];
                        }

                        over.endText();
                        over.beginText();
                        over.setFontAndSize(bf, 8);
                        if(annotations.length > 3) {
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
                            over.setTextMatrix(spacing + 10, 10);
                            over.showText("(" + annot.substring(annot.indexOf(':') + 2) + ")");
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

    private static String convertToPdfIfImage(String filename) {
        String[] imageFormats = {".jpg",".jpeg",".png",".tif",".bmp",".gif",".svg",".tiff"};
        String format = filename.substring(filename.lastIndexOf("."));
        String newFilename = filename;
        try {
            if (Arrays.asList(imageFormats).contains(format)) {
                Document document = new Document();
                newFilename = filename.replace(format, ".pdf");
                PdfWriter.getInstance(document, new FileOutputStream(downloadPath + newFilename));
                document.open();
                Image img = Image.getInstance(downloadPath  + filename);
                float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / img.getWidth()) * 100;
                img.scalePercent(scaler);
                document.add(img);
                document.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newFilename;
    }

    public JSONObject processRequest(String payload) {
        System.out.println("--PROCESSING PAYLOAD ----");
        StringWriter errors = new StringWriter();
        JSONObject jsonObject = AnnotateDocumentsProcessor.decodeJson(payload);
        JSONObject result = new JSONObject();
        Boolean error = false;
        String rid = (String) jsonObject.get("rid");
        JSONArray files = (JSONArray) jsonObject.get("files");
        Iterator<JSONObject> iterator = files.iterator();
        while(iterator.hasNext()) {
            JSONObject file = iterator.next();
            try {
                annotateFile(file);
            } catch (Exception e) {
                error = true;
                e.printStackTrace();
                e.printStackTrace(new PrintWriter(errors));
            }
        }
        try {
            result = mergeFiles(files, rid);
        } catch (Exception e) {
            error = true;
            e.printStackTrace();
            e.printStackTrace(new PrintWriter(errors));
        }
        JSONObject response = new JSONObject();
        response.put("topic", rid);
        if(error == true) {
            result.put("error", errors.toString());
        } 
        response.put("message", result.toString());
        System.out.println("---MESSAGE PROCESSED -----");
        return response;
    }


	

	public static JSONObject mergeFiles(JSONArray files, String rid) throws Exception {
        Document document = new Document();
        FileOutputStream outputStream = new FileOutputStream(uploadPath + rid + ".pdf");
        PdfCopy copy = new PdfSmartCopy(document, outputStream);
        document.open();
        Iterator<JSONObject> iterator = files.iterator();
        while(iterator.hasNext()) {
            final JSONObject fileObject = iterator.next();
            String filename = (String) fileObject.get("file_name");
            CustomReader reader = new CustomReader(uploadPath + filename);
            copy.addDocument(reader);
            copy.freeReader(reader);
            reader.close();
        }
        document.close();
        String url = AmazonS3Util.uploadFile(rid + ".pdf");
        JSONObject result = new JSONObject();
        result.put("file_url", url);
        result.put("file_name", rid + ".pdf");
        return result;
    }
}




class CustomReader extends PdfReader {
    public CustomReader(String filename) throws IOException {
            super(filename);
    }
    public CustomReader(String filename, byte password[]) throws IOException {
            super(filename, password);
    }
    public void decryptOnPurpose() {
        encrypted = false;
    }
}