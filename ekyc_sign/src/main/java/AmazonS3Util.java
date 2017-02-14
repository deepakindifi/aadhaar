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
    private static final String s3BucketName = "docs-indifi";
    private static final String esignDocumentSuffix = "_esign";


    public static void downloadFile(String fileName, String httpUrl) {

        try {
            File destinationFile = new File(downloadFolder + "/" + fileName);
            URL url = new URL(httpUrl);
            GetObjectRequest getObjectRequest = new GetObjectRequest(s3BucketName, url.getPath().substring(1));
            s3Client.getObject(getObjectRequest, destinationFile);
        } catch (Exception e) {

        }

    }

    public static String uploadFile(String fileName, String httpUrl) {

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


}
