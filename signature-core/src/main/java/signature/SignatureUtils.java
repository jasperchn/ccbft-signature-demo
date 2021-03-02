package signature;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import signature.configurable.Config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;

import static signature.constants.Marker.*;

public class SignatureUtils {

    public static String generateNonce() {
        return RandomStringUtils.randomAlphabetic(Config.NONCE_LENGTH);
    }

    public static long generateTimestamp() {
        return (new Date()).getTime() + Config.TIMESTAMP_BIAS;
    }

    public static String buildBase64Md5(byte[] bytes) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance(MD5).digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildBase64Md5(String bodyString) {
        try {
            return buildBase64Md5(bodyString.getBytes(ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String sign(String appSecret, String toBeSign) {
        try {
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            byte[] key = appSecret.getBytes(ENCODING);
            hmacSha256.init(new SecretKeySpec(key, 0, key.length, HMAC_SHA256));
            return new String(Base64.getEncoder().encode(hmacSha256.doFinal(toBeSign.getBytes(ENCODING))), ENCODING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * contentType 指报文类型，例如application/json；多数框架有对应的枚举类
     * application/json
     * application/json;charset=UTF-8
     */
    private static Map<String, String> processBodyString(String bodyString, String contentType) {
        if (contentType.startsWith(CONTENT_TYPE_URLENCODED)) {
            return processBodyStringUrlEncoded(bodyString);
        } else {
            return new HashMap<>();
        }
    }

    private static Map<String, String> processBodyStringUrlEncoded(String bodyString) {
        try {
            Map<String, String> map = new HashMap<>();
            int ptr;
            for (String seg : bodyString.split(SPE3)) {
                if (!map.containsKey(seg)) {
                    ptr = seg.indexOf(SPE4);
                    map.put(seg.substring(0, ptr), seg.substring(ptr + 1));
                }
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     */
    public static String buildBodyMd5(String contentType, String bodyString) {
        if (!bodyString.startsWith("{}") && !contentType.startsWith(CONTENT_TYPE_URLENCODED)) {
            return buildBase64Md5(bodyString);
        } else {
            return "";
        }
    }

    /**
     * bodyString body转为字符串
     * queries url参数key-value对
     * path url
     * 多数开发框架中可以从封装对象中取出以上值，例如Spring Boot的ServerHttpRequest
     */
    public static String buildQueries(String contentType, String bodyString, String path, Map<String, String> queries) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder paramsBuilder = new StringBuilder();
        Map<String, String> sortMap = new TreeMap<>();

        stringBuilder.append(path);
        if (queries != null) {
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                if (StringUtils.isNotBlank(entry.getKey())) sortMap.put(entry.getKey(), entry.getValue());
            }
        }

        if (StringUtils.isNotBlank(bodyString)) {
            sortMap.putAll(processBodyString(bodyString, contentType));
        }

        for (Map.Entry<String, String> entry : sortMap.entrySet()) {
            if (StringUtils.isNotBlank(entry.getKey())) {
                if (paramsBuilder.length() > 0) paramsBuilder.append(SPE3);
                paramsBuilder.append(entry.getKey()).append(SPE4);
                if (StringUtils.isNotBlank(entry.getValue())) paramsBuilder.append(entry.getValue());
            }
        }

        if (paramsBuilder.length() > 0) {
            stringBuilder.append(SPE5);
            stringBuilder.append(paramsBuilder);
        }

        return stringBuilder.toString();
    }

    /**
     * method always UpperCase
     * content md5 --> appkey
     */
    public static String buildHeaders(String method, String contentType, String bodyString) {
        StringJoiner stringJoiner = new StringJoiner(LF)
                .add(method.toUpperCase())
                .add(buildBodyMd5(contentType, bodyString))
                .add(contentType)
                .add(String.valueOf(generateTimestamp()))
                .add(generateNonce())
                .add(Config.appKey);
        return stringJoiner.toString();
    }

    /**
     * 服务端对x-ca-顺序没有排序要求
     */
    private static class SignatureAddons {
        private String addonsHeaders;
        private String addonsSignature;

        public String getAddonsHeaders() {
            return addonsHeaders;
        }

        public void setAddonsHeaders(String addonsHeaders) {
            this.addonsHeaders = addonsHeaders;
        }

        public String getAddonsSignature() {
            return addonsSignature;
        }

        public void setAddonsSignature(String addonsSignature) {
            this.addonsSignature = addonsSignature;
        }

        public SignatureAddons(String addonsHeaders, String addonsSignature) {
            this.addonsHeaders = addonsHeaders;
            this.addonsSignature = addonsSignature;
        }
    }


    /**
     * 允许x-ca-开头的header参与计算，拓展位
     */
    public static SignatureAddons buildHeaderAddons(Map<String, String> addons) {
        if (null == addons || addons.size() <= 0) return new SignatureAddons("", LF);
        StringJoiner headers = new StringJoiner(SPE1);
        StringBuilder signature = new StringBuilder();
        for (Map.Entry<String, String> entry : addons.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                headers.add(entry.getKey());
                signature.append(entry.getValue()).append(LF);
            }
        }
        return new SignatureAddons(headers.toString(), signature.toString());
    }

    /**
     * textToSign/Raw
     * ------------------------
     * GET
     * lgssPcOh82g9MuajwOiEBA==
     * application/json
     * 1614322305268
     * d13c26e5-198c-4dbb-9676-71de45c67b7b
     * Tester-Key
     * x-ca-add:12123123123
     * x-ca-signature-method:HmacSHA256
     * /sts/publicrs/oauth/login/jscode?h5Id=206893815594889216&workspaceId=test
     */
    public static String buildSignature(String method, String path, String contentType, String bodyString, Map<String, String> headerAddons, Map<String, String> queries) {

        SignatureAddons signatureAddons = buildHeaderAddons(headerAddons);
        StringJoiner stringJoiner = new StringJoiner(LF)
                .add(buildHeaders(method, contentType, bodyString))
                .add(signatureAddons.getAddonsHeaders())
                .add(buildQueries(contentType, bodyString, path, queries));
        String tobeSign = stringJoiner.toString();
        System.out.println("------------- raw text to sign --------------\n" + tobeSign);
        return sign(Config.appSecret, tobeSign);
    }

}
