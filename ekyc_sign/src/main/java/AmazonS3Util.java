import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Sahil on 14/02/17.
 */
public class AmazonS3Util {

    private static final BasicAWSCredentials s3Credentials = new BasicAWSCredentials(System.getProperty("aws.access.key"), System.getProperty("aws.access.secret"));
    private static final AmazonS3Client s3Client = new AmazonS3Client(s3Credentials);
    private static final String downloadFolder = "src/main/resources/downloads";
    private static final String uploadFolder = "src/main/resources/uploads";
    private static final String defaultS3BucketName = "docs-indifi-staging";
    private static final String documentHashFolder = "src/main/resources/hash-files";
    private static final String esignResponseFolder = "src/main/resources/response-files";
    private static final String responseS3BucketKey = "esign-response";
    private static final String s3BucketName = "docs-indifi-staging";
    private static final String esignDocumentSuffix = "-esign";

    public static void downloadESignFile(String fileName, String httpUrl) {

        try {
            File destinationFile = new File(downloadFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            GetObjectRequest getObjectRequest = new GetObjectRequest(s3BucketName, url.getPath().substring(1));
            s3Client.getObject(getObjectRequest, destinationFile);
        } catch (Exception e) {

        }

    }

    public static String uploadESignFile(String fileName, String httpUrl) {

        try {
            File sourceFile = new File(uploadFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            String newFileName = new StringBuilder(fileName).insert(fileName.lastIndexOf("."), esignDocumentSuffix).toString();
            String s3BucketKey = url.getPath().substring(1, url.getPath().lastIndexOf("/")) + "/" + newFileName;
            String newHttpUrl = httpUrl.substring(0, httpUrl.lastIndexOf("/")) + "/" + newFileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, s3BucketKey, sourceFile);
            s3Client.putObject(putObjectRequest);
            return newHttpUrl;
        } catch (Exception e) {

        }
        return null;

    }

    public static String uploadDocumentHashFile(String fileName, String httpUrl) {

        try {
            File sourceFile = new File(documentHashFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            String s3BucketKey = url.getPath().substring(1, url.getPath().lastIndexOf("/")) + "/" + fileName;
            String hashHttpUrl = httpUrl.substring(0, httpUrl.lastIndexOf("/")) + "/" + fileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, s3BucketKey, sourceFile);
            s3Client.putObject(putObjectRequest);
            return hashHttpUrl;
        } catch (Exception e) {

        }
        return null;

    }

    public static String uploadESignResponse(String fileName, String httpUrl) {

        try {
            File sourceFile = new File(esignResponseFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            String s3BucketKey = responseS3BucketKey + "/" + fileName;
            String responseHttpUrl = "https://" + url.getHost() + "/" + s3BucketKey;
            PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, s3BucketKey, sourceFile);
            s3Client.putObject(putObjectRequest);
            return responseHttpUrl;
        } catch (Exception e) {

        }
        return null;

    }

    public static void downloadFile(String fileName, String httpUrl) {
        AmazonS3Util.downloadFile(fileName, httpUrl, defaultS3BucketName);
    }

    public static void downloadFile(String fileName, String httpUrl, String s3BucketName) {
        try {
            File destinationFile = new File(downloadFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            GetObjectRequest getObjectRequest = new GetObjectRequest(s3BucketName, url.getPath().substring(1));
            s3Client.getObject(getObjectRequest, destinationFile);
        } catch (Exception e) {
            System.out.println("exception is " + e.getMessage());
        }
    }


    public static String uploadFile(String fileName, String httpUrl) {
        return AmazonS3Util.uploadFile(fileName, httpUrl, defaultS3BucketName);
    }

    public static String uploadFile(String fileName, String httpUrl, String s3BucketName) {
        try {
            File sourceFile = new File(uploadFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            String s3BucketKey = url.getPath().substring(1, url.getPath().lastIndexOf("/")) + "/" + fileName;
            String newHttpUrl = httpUrl.substring(0, httpUrl.lastIndexOf("/")) + "/" + fileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, s3BucketKey, sourceFile);
            s3Client.putObject(putObjectRequest);
            return newHttpUrl;

        } catch (Exception e) {
            System.out.println("exception is " + e.getMessage());
        }

        return null;
    }

}
