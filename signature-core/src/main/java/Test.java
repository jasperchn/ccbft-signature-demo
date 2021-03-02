import signature.SignatureUtils;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static signature.constants.HttpHeader.CA_HEADER_TO_SIGN_PREFIX;

public class Test {

    public static void main(String[] args) throws URISyntaxException {
        // URI uri = new URI("http://11.10/usr/dda/djj?p1=v1&p2=v2");
        System.out.println("------------ default test ----------");
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

        System.out.println(SignatureUtils.buildSignature(method, path, bodyType, bodyString, addons, queries));

    }
}
