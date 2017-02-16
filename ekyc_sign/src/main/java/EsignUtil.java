import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Created by Sahil on 16/02/17.
 */
public class EsignUtil {

    public static String addSuffixToFilename(String filename, String suffix) {

        String newFileName = new StringBuilder(filename).insert(filename.lastIndexOf("."), suffix).toString();
        return newFileName;
    }

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
}
