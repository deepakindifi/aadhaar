import org.json.simple.*;

public interface RequestProcessor {
   JSONObject processRequest(String payload);
}