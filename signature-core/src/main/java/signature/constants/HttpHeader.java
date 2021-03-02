package signature.constants;

/**
 * @Author: zhengwentao
 * @Date: 2019/9/27 10:52
 */


public class HttpHeader {
    /**
     * 以下为默认字段
     */
    public static final String HTTP_HEADER_ACCEPT = "accept";

    public static final String HTTP_HEADER_CONTENT_MD5 = "contentMd5";

    public static final String HTTP_HEADER_CONTENT_TYPE = "content-type";

    // public static final String HTTP_HEADER_CONTENT_LENGTH = "content-length";

    // date弃用，使用timestamp作为时间盐即可
    // public static final String HTTP_HEADER_DATE = "date";

    public static final String HTTP_HEADER_NONCE = "nonce";

    public static final String HTTP_HEADER_TIMESTAMP = "timestamp";

    public static final String HTTP_HEADER_APPKEY = "appKey";

    /**
     * 以下为签名字段
     */
    public static final String HTTP_HEADER_SIGNATURE = "signature";

    public static final String HTTP_HEADER_SIGNATURE_HEADERS = "signatureHeaders";

    /**
     * 以下为拓展字段前缀
     */
    public static final String CA_HEADER_TO_SIGN_PREFIX = "x-ca-";
}
