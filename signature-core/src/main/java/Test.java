import signature.SignatureUtils;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static signature.constants.HttpHeader.CA_HEADER_TO_SIGN_PREFIX;

public class Test {

    private static class Config {
        public static final long TIMESTAMP_BIAS = 0;
        public static final int NONCE_LENGTH = 31;
        public static final String appKey = "common";
        public static final String appSecret = "f554a080655952d8354d42d0ef1ec4b0175239e5";
    }

    public static void main(String[] args) throws URISyntaxException {
        System.out.println("------------ default test ----------");
        String timestamp = SignatureUtils.generateTimestamp(Config.TIMESTAMP_BIAS);
        String nonce = SignatureUtils.generateNonce(Config.NONCE_LENGTH);
        String method = "post";
        String path = "/usr/false/testing";
        String bodyType = "application/json";
        String bodyString = "{\n    \"body0\": \"bv0\",\n    \"body1\": \"bv1\"\n}";
        Map<String, String> addons = new HashMap<String, String>() {{
            put(CA_HEADER_TO_SIGN_PREFIX + "addon1", "dummy1");
            put(CA_HEADER_TO_SIGN_PREFIX + "addon2", "dummy2");
            put(CA_HEADER_TO_SIGN_PREFIX + "addon3", "dummy3");
        }};
        Map<String, String> queries = new HashMap<String, String>() {{
            put("param1", "value1");
            put("param2", "value2");
            put("param3", "value3");
        }};

        SignatureUtils.SignatureInfo signatureInfo =
                SignatureUtils.buildSignature(
                        method,
                        path,
                        bodyType,
                        bodyString,
                        timestamp,
                        nonce,
                        Config.appKey,
                        Config.appSecret,
                        addons,
                        queries
                );
        System.out.println(signatureInfo.toString());
    }
}
