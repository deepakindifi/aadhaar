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
		} catch(Exception ex) {return null;}
	}

	public static JSONObject esign(String payload) {
        try {
            JSONObject jsObj = ProcessESignRequest.decodeJson(pl);
            String pid = (String)jsObj.get("pid");
            String auth = (String)jsObj.get("auth");
            String otp = (String)jsObj.get("otp");
            String city = (String)jsObj.get("city");
            String aadhaar = (String)jsObj.get("aadhaar");
            JSONArray filenames = (JSONArray)jsObj.get("filenames");
            JSONArray reason = (JSONArray)jsObj.get("reason");

            eSign esign = new eSign("src/main/resources/indifi.lic", "src/main/resources/indifi.pfx", "emudhra", false, "", 0,0);

            ServiceReturn serviceReturn = new ServiceReturn();
            List<eSignInputs> esigninputsList = new ArrayList<eSignInputs>();
            for(int index = 0; index < filenames.length; index++) {
                byte[] toBesignedData = null;
                byte[] pdfAsByteArray = null;
                FileInputStream fis = new FileInputStream("src/main/resources/downloads/" + filenames[index]);
                pdfAsByteArray = new byte[fis.available()];
                fis.read(pdfAsByteArray);
                fis.close();
                toBesignedData = Base64.getEncoder().encode(pdfAsByteArray);
                eSignInputs esInputs = new eSignInputs(new String(toBesignedData), city, reason[index], false, eSignImpl.PageTobeSigned.Last, eSignImpl.Coordinates.BottomRight,null, "");
                esigninputsList.add(esInputs);
            }
            serviceReturn = esign.BulkeSign(aadhaar, otp, eSignImpl.eSign_Auth.OTP, "UNIQUE_DEVICE_CODE_FOR_BIO", esigninputsList, "/home/ubuntu/emudhra_new");
            System.out.println(serviceReturn.getStatus());
            System.out.println(serviceReturn.getResponseXML());

                //Option 2 
                //For Pre eKYC based pdf signing 
                // serviceReturn = esign.Bulkesign(esignImpl.esign_Auth.Biometric, esigninputsList, "Temp_Path_For_Internal_Use_Of_Application", "eKycResXML"); 

            Iterator<String> itr = serviceReturn.getSignedPDFContentArr().iterator();
            String[] data = new String[10];
            int i = 0;
            while (itr.hasNext()) {
                data[i] = itr.next();
                FileOutputStream fileOutputStream = new FileOutputStream("src/main/resources/uploads/"+filenames[i]);
                fileOutputStream.write(Base64.getDecoder().decode(data[i]));
                fileOutputStream.flush();
                fileOutputStream.close();
                i++;
            }
            JSONObject response = new JSONObject();
            response.put("pid", pid);
            response.put("response",serviceReturn.getResponseXML());
            return response;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }


    }
