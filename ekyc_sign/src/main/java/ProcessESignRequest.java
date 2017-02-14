import com.emudhra.esign.*;
import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public final class ProcessESignRequest {

    public static JSONObject decodeJson(String data) {
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(data);
            JSONObject jsObj = (JSONObject) obj;
            return jsObj;
        } catch (Exception ex) {
            return null;
        }
    }

    public static JSONObject esign(String payload) {
        try {
            JSONObject jsObj = ProcessESignRequest.decodeJson(payload);
            String pid = (String) jsObj.get("pid");
            String auth = (String) jsObj.get("auth");
            String otp = (String) jsObj.get("otp");
            String city = (String) jsObj.get("city");
            String aadhaar = (String) jsObj.get("aadhaar");
            JSONArray mergedDocuments = (JSONArray) jsObj.get("merged_documents");
            Map<Integer, JSONObject> mergedDocumentsMap = new HashMap<Integer, JSONObject>();

            eSign esign = new eSign("src/main/resources/indifi.lic", "src/main/resources/indifi.pfx", "emudhra", false, "", 0, 0);

            ServiceReturn serviceReturn = new ServiceReturn();
            List<eSignInputs> esigninputsList = new ArrayList<eSignInputs>();
            Iterator<JSONObject> iterator = mergedDocuments.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                JSONObject fileObject = iterator.next();
                String url = (String) fileObject.get("url");
                String reason = (String) fileObject.get("reason");
                String filename = (String) fileObject.get("name");
                AmazonS3Util.downloadFile(filename, url);
                byte[] toBesignedData = null;
                byte[] pdfAsByteArray = null;
                FileInputStream fis = new FileInputStream("src/main/resources/downloads/" + filename);
                pdfAsByteArray = new byte[fis.available()];
                fis.read(pdfAsByteArray);
                fis.close();
                toBesignedData = Base64.getEncoder().encode(pdfAsByteArray);
                eSignInputs esInputs = new eSignInputs(new String(toBesignedData), city, reason, false, eSignImpl.PageTobeSigned.Last, eSignImpl.Coordinates.BottomRight, null, "");
                esigninputsList.add(esInputs);
                fileObject.put("document_hash", toBesignedData);
                mergedDocumentsMap.put(i++, fileObject);

            }

            serviceReturn = esign.BulkeSign(aadhaar, otp, eSignImpl.eSign_Auth.OTP, "UNIQUE_DEVICE_CODE_FOR_BIO", esigninputsList, "/home/ubuntu/emudhra_new");
            System.out.println(serviceReturn.getStatus());
            System.out.println(serviceReturn.getResponseXML());

            //Option 2
            //For Pre eKYC based pdf signing
            // serviceReturn = esign.Bulkesign(esignImpl.esign_Auth.Biometric, esigninputsList, "Temp_Path_For_Internal_Use_Of_Application", "eKycResXML");

            Iterator<String> itr = serviceReturn.getSignedPDFContentArr().iterator();
            String[] data = new String[10];
            i = 0;
            JSONArray esignedDocuments = new JSONArray();
            while (itr.hasNext()) {
                data[i] = itr.next();
                File file = new File("src/main/resources/uploads/" + (String)mergedDocumentsMap.get(i).get("name"));
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(Base64.getDecoder().decode(data[i]));
                fileOutputStream.flush();
                fileOutputStream.close();
                String httpUrl = AmazonS3Util.uploadFile((String)mergedDocumentsMap.get(i).get("name"), (String)mergedDocumentsMap.get(i).get("url"));
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", (String)mergedDocumentsMap.get(i).get("id"));
                jsonObject.put("esigned_url", httpUrl);
                jsonObject.put("document_hash", (String)mergedDocumentsMap.get(i).get("document_hash"));
                esignedDocuments.add(jsonObject);
                i++;
            }
            JSONObject response = new JSONObject();
            response.put("pid", pid);
            response.put("response", serviceReturn.getResponseXML());
            response.put("merged_documents", esignedDocuments);

            return response;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    return null;
    }
}
