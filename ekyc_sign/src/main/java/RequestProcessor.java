import org.json.simple.JSONObject;

public interface RequestProcessor {
   JSONObject processRequest(String payload);
}