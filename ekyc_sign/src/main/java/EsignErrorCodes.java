import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sahil on 17/02/17.
 */
public class EsignErrorCodes {

    public static String INCORRECT_OTP = "OTP_INVALID";
    public static String INVALID_PID = "PID_INVALID";
    public static String ESIGN_FAILED = "ESIGN_FAILED";
    public static Map<String, String> errorCodeMap = new HashMap<String, String>();

    static {
        //esign error code mapping
        errorCodeMap.put("400", INCORRECT_OTP);
        errorCodeMap.put("511", INVALID_PID);
    }

    public static String getErrorCode(String esignErrorCode) {
        String errorCode = errorCodeMap.get(esignErrorCode) != null ? errorCodeMap.get(esignErrorCode) : ESIGN_FAILED;
        return errorCode;
    }

}
