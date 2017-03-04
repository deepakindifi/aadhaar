import org.json.simple.*;
import org.json.simple.parser.*;

public interface RequestProcessor {
   JSONObject processRequest();
}