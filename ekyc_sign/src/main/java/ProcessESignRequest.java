import com.emudhra.esign.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.simple.*;
import org.json.simple.parser.*;
import scala.util.parsing.json.JSON;

public final class ProcessESignRequest {

    private static final ExecutorService threadpool = Executors.newFixedThreadPool(10);
    private static final String documentHashFileSuffix = "-hash";
    private static final String responseFileSuffix = "-response";



    public static JSONObject esign(String payload) {
	JSONObject response = new JSONObject();
        try {
            JSONObject jsObj = EsignUtil.decodeJson(payload);
            final String pid = (String) jsObj.get("esign_id");

            String auth = (String) jsObj.get("auth");
            String otp = (String) jsObj.get("otp");
            String city = (String) jsObj.get("city");
            String aadhaar = (String) jsObj.get("aadhaar");
            JSONArray documents = (JSONArray) jsObj.get("documents");
            final String s3Url = (String) ((JSONObject)documents.iterator().next()).get("generated_url");
            List<JSONObject> documentsList = new ArrayList<JSONObject>();

            eSign esign = new eSign("src/main/resources/indifi.lic", "src/main/resources/indifi.pfx", "emudhra", false, "", 0, 0);

            ServiceReturn serviceReturn = new ServiceReturn();
            List<eSignInputs> esignInputsList = new ArrayList<eSignInputs>();
            Iterator<JSONObject> iterator = documents.iterator();
            int i = 0;
            List<Future<eSignInputs>> eSignInputsFutureList = new ArrayList<>();
            while (iterator.hasNext()) {

                final JSONObject fileObject = iterator.next();
                String url = (String) fileObject.get("generated_url");
                String reason = (String) fileObject.get("reason");
                String filename = EsignUtil.getFileNameFromUrl(url);
                Future eSignInputsFuture = threadpool.submit(new Callable<eSignInputs>() {

                    @Override
                    public eSignInputs call() {
                        try {
                            AmazonS3Util.downloadESignFile(filename, url);
                            byte[] toBesignedData = null;
                            byte[] pdfAsByteArray = null;
                            FileInputStream fis = new FileInputStream("src/main/resources/downloads/" + filename);
                            pdfAsByteArray = new byte[fis.available()];
                            fis.read(pdfAsByteArray);
                            fis.close();
                            toBesignedData = Base64.getEncoder().encode(pdfAsByteArray);
                            fileObject.put("document_hash", new String(toBesignedData));
                            eSignInputs esInputs = new eSignInputs(new String(toBesignedData), city, reason, false, eSignImpl.PageTobeSigned.Last, eSignImpl.Coordinates.BottomRight, null, "");
                            return esInputs;
                        } catch (Exception e) {

                        }
                        return null;

                    }
                });

                eSignInputsFutureList.add(eSignInputsFuture);
                documentsList.add(fileObject);
            }

            for(Future<eSignInputs> eSignInputsFuture : eSignInputsFutureList) {

                esignInputsList.add(eSignInputsFuture.get());
            }

	        System.out.println("length is " + esignInputsList.size());
            serviceReturn = esign.BulkeSign(aadhaar, otp, eSignImpl.eSign_Auth.OTP, "UNIQUE_DEVICE_CODE_FOR_BIO", esignInputsList, "src/main/resources/uploads");
            System.out.println(serviceReturn.getStatus());
            System.out.println(serviceReturn.getResponseXML());

            response.put("esign_id", pid);
            response.put("response", serviceReturn.getResponseXML());

            //Option 2
            //For Pre eKYC based pdf signing
            // serviceReturn = esign.Bulkesign(esignImpl.esign_Auth.Biometric, esigninputsList, "Temp_Path_For_Internal_Use_Of_Application", "eKycResXML");

            if(serviceReturn.getStatus().equalsIgnoreCase("success")) {

                //upload document hash files to S3
                for(final JSONObject document : documentsList) {
                   threadpool.submit(new Runnable() {
                       @Override
                       public void run() {

                           try {
                               String documentUrl = (String)document.get("generated_url");
                               String filename = EsignUtil.getFileNameFromUrl(documentUrl);
                               String hashFilename = filename.substring(0, filename.lastIndexOf(".")) + documentHashFileSuffix + ".txt";
                               File hashFile = new File("src/main/resources/hash-files/" + hashFilename);
                               PrintWriter out = new PrintWriter(hashFile);
                               out.println((String)document.get("document_hash"));
                               String httpUrl = AmazonS3Util.uploadDocumentHashFile(hashFilename, documentUrl);
                               document.put("document_hash_url", httpUrl);
                           } catch (Exception e) {

                           }

                       }
                   });
                }

                final String responseXML = serviceReturn.getResponseXML();
                //upload esign response file
                Future<String> responseUploadFuture = threadpool.submit(new Callable<String>() {
                    @Override
                    public String call() {

                        try {
                            String filename = pid + responseFileSuffix + ".txt";
                            File responseFile = new File("src/main/resources/response-files/" + filename);
                            PrintWriter out = new PrintWriter(responseFile);
                            out.println(responseXML);
                            String httpUrl = AmazonS3Util.uploadESignResponse(filename, s3Url);
                            return httpUrl;
                        } catch (Exception e) {

                        }
                        return null;
                    }
                });


                List<Future<JSONObject>> signedDocumentsFutureList = new ArrayList<>();
                Iterator<String> itr = serviceReturn.getSignedPDFContentArr().iterator();
                JSONArray signedDocuments = new JSONArray();
                i = 0;
                while (itr.hasNext()) {
                    final String data = itr.next();
                    final String url = (String)documentsList.get(i).get("generated_url");
                    final String filename = EsignUtil.getFileNameFromUrl(url);
                    final String documentId = (String)documentsList.get(i).get("id");
                    final String documentHashUrl = (String)documentsList.get(i).get("document_hash_url");
                    File file = new File("src/main/resources/uploads/" + filename);
                    final JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", documentId);
                    Future signedDocumentsFuture = threadpool.submit(new Callable<JSONObject>() {

                        @Override
                        public JSONObject call() {
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(file);
                                fileOutputStream.write(Base64.getDecoder().decode(data));
                                fileOutputStream.flush();
                                fileOutputStream.close();
                                String httpUrl = AmazonS3Util.uploadESignFile(filename, url);

                                jsonObject.put("signed_url", httpUrl);
                                jsonObject.put("document_hash_url", documentHashUrl);
                            } catch (Exception e) {

                            }

                            return jsonObject;
                        }
                    });

                    signedDocumentsFutureList.add(signedDocumentsFuture);
                    i++;
                }

                for(Future<JSONObject> signedDocumentFuture : signedDocumentsFutureList) {
                    signedDocuments.add(signedDocumentFuture.get());
                }
                response.put("esign_id", pid);
                response.put("response_url", responseUploadFuture.get());
                response.put("documents", signedDocuments);
                response.put("transaction_id",serviceReturn.getTxnNo());
                response.put("status","success");
                response.put("topic", (String) jsObj.get("topic"));
            } else {
                response.put("status","failure");
                response.put("error_code", "");
            }

        } catch (Exception ex) {
            System.out.println("exception " + ex.getMessage());
            response.put("esign_id", "");
            response.put("status","failure");
	        ex.printStackTrace();
        }


        return response;
    }
}
