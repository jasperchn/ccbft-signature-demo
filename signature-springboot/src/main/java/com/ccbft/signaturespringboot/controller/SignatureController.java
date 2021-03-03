package com.ccbft.signaturespringboot.controller;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;
import reactor.core.publisher.Mono;
import signature.SignatureUtils;
import signature.constants.HttpHeader;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;



@RestControllerAdvice
@Slf4j
@RequestMapping("/common")
public class SignatureController {

    private static final String host = "http://103.126.126.119:8002/phjr";

    private static class Config {
        public static final long TIMESTAMP_BIAS = 0;
        public static final int NONCE_LENGTH = 31;
        public static final String appKey = "common";
        public static final String appSecret = "f554a080655952d8354d42d0ef1ec4b0175239e5";
    }

    @Getter
    public enum UriMaps {
        Test_GET(HttpMethod.GET, "/bds/signature/tester"),
        Test_DELETE(HttpMethod.DELETE, "/bds/signature/tester"),
        Test_PUT_WithoutBody(HttpMethod.PUT,"bds/signature/tester/withoutbody"),
        Test_PUT_WithBody(HttpMethod.PUT, "/bds/signature/tester/withbody"),
        Test_POST_WithoutBody(HttpMethod.POST,"bds/signature/tester/withoutbody"),
        Test_POST_WithBody(HttpMethod.POST, "/bds/signature/tester/withbody"),
        Test_POST_Multipart_WithoutParams(HttpMethod.POST, "/bds/signature/tester/multipart/withoutparams"),
        Test_POST_Multipart_WithParams(HttpMethod.POST, "/bds/signature/tester/multipart/withparams");

        private HttpMethod httpMethod;
        private String route;

        UriMaps(HttpMethod httpMethod, String route) {
            this.httpMethod = httpMethod;
            this.route = route;
        }
    }

    @Data
    @NoArgsConstructor
    public static class AuthInfo{
        private String authKey;
        private String authValue;

        public AuthInfo(String authKey, String authValue) {
            this.authKey = authKey;
            this.authValue = authValue;
        }
    }

    private AuthInfo handleAuth(String token, String authorization) {
        Assert.isTrue(StringUtils.hasText(token) ^ StringUtils.hasText(authorization), "provide either token or authorization");
        if(StringUtils.hasText(token))
            return new AuthInfo("token", "Bearer " + token);
        else
            return new AuthInfo("Authorization", "Bearer " + authorization);
    }


    /**
     * Arrays.asList() is immutable
     * */
    private <K, V> MultiValueMap<K, V> convertMultiple(Map<K, V> single) {
        MultiValueMap<K, V> r = new LinkedMultiValueMap<>();
        for(Map.Entry<K, V> entry : single.entrySet()) {
            r.put(entry.getKey(), new ArrayList<V>(){{add(entry.getValue());}});
        }
        return r;
    }

    private Mono<String> defaultNoBody(UriMaps uriMaps, Map<String, String> queries, AuthInfo authInfo, ServerWebExchange webExchange){
        /**
         * 计算签名
         * */
        String timestamp = SignatureUtils.generateTimestamp(Config.TIMESTAMP_BIAS);
        String nonce = SignatureUtils.generateNonce(Config.NONCE_LENGTH);
        String signature = SignatureUtils.buildSignature(
                uriMaps.getHttpMethod().name(),
                uriMaps.getRoute(),
                "",
                "",
                timestamp,
                nonce,
                Config.appKey,
                Config.appSecret,
                Collections.emptyMap(),
                queries
        ).getSignature();
        log.info("signature = {}", signature);

        /**
         * 放置Header
         * */
        Consumer<HttpHeaders> headersConsumer = httpHeaders -> {
            httpHeaders.add(authInfo.getAuthKey(), authInfo.getAuthValue());
            httpHeaders.add(HttpHeader.HTTP_HEADER_NONCE, nonce);
            httpHeaders.add(HttpHeader.HTTP_HEADER_APPKEY, Config.appKey);
            httpHeaders.add(HttpHeader.HTTP_HEADER_TIMESTAMP, timestamp);
            httpHeaders.add(HttpHeader.HTTP_HEADER_SIGNATURE, signature);
        };

        UriBuilderFactory factory = new DefaultUriBuilderFactory();
        URI uri = factory.builder().host(host).path(uriMaps.getRoute()).queryParams(convertMultiple(queries)).build().normalize();
        return WebClient
                .create(uri.getAuthority())
                .get()
                .uri(StringUtils.hasText(uri.getQuery()) ? uri.getPath() + "?" + uri.getQuery() : uri.getPath())
                .headers(headersConsumer)
                .exchangeToMono(clientResponse -> {
                    webExchange.getResponse().setStatusCode(clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class);
                });
    }

    Mono<String> defaultWithBody(UriMaps uriMaps, Map<String, String> queries, AuthInfo authInfo, ServerWebExchange webExchange) {
        /**
         * 计算签名
         * */
        return DataBufferUtils.join(webExchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes);
                })
                .flatMap(bodyString -> {
                    // String bodyMd5 = SignatureUtils.buildBase64Md5(bodyString);
                    String timestamp = SignatureUtils.generateTimestamp(Config.TIMESTAMP_BIAS);
                    String nonce = SignatureUtils.generateNonce(Config.NONCE_LENGTH);
                    String contentType = MediaType.APPLICATION_JSON_VALUE;
                    SignatureUtils.SignatureInfo signatureInfo = SignatureUtils.buildSignature(
                            uriMaps.getHttpMethod().name(),
                            uriMaps.getRoute(),
                            contentType,      // content-type 可以强行指定，此处强行指定为json，更好的办法应该是用拦截器/修饰器最后从已知的content-type中取
                            bodyString,
                            timestamp,
                            nonce,
                            Config.appKey,
                            Config.appSecret,
                            Collections.emptyMap(),
                            queries
                    );

                    log.info("bodyString={}", bodyString);
                    log.info("bodyMd5={}", signatureInfo.getContentDigest());
                    log.info("signature = {}", signatureInfo.getSignature());

                    Consumer<HttpHeaders> headersConsumer = httpHeaders -> {
                        httpHeaders.add(authInfo.getAuthKey(), authInfo.getAuthValue());
                        httpHeaders.add(HttpHeader.HTTP_HEADER_NONCE, nonce);
                        httpHeaders.add(HttpHeader.HTTP_HEADER_APPKEY, Config.appKey);
                        httpHeaders.add(HttpHeader.HTTP_HEADER_TIMESTAMP, timestamp);
                        httpHeaders.add(HttpHeader.HTTP_HEADER_CONTENT_MD5, signatureInfo.getContentDigest());
                        httpHeaders.add(HttpHeader.HTTP_HEADER_CONTENT_TYPE, contentType);
                        httpHeaders.add(HttpHeader.HTTP_HEADER_SIGNATURE, signatureInfo.getSignature());
                    };

                    URI uri = (new DefaultUriBuilderFactory()).builder().host(host).path(uriMaps.getRoute()).queryParams(convertMultiple(queries)).build().normalize();
                    return WebClient
                            .create(uri.getAuthority())
                            .post()
                            .uri(StringUtils.hasText(uri.getQuery()) ? uri.getPath() + "?" + uri.getQuery() : uri.getPath())
                            .body(BodyInserters.fromValue(bodyString))
                            .headers(headersConsumer)
                            .exchangeToMono(clientResponse -> {
                                webExchange.getResponse().setStatusCode(clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class);
                            });
                });
    }

    /**
     * token为短token
     * authorization为长token
     * 二选一即可
     * */
    @GetMapping("/test/get")
    public Mono<String> getTest_01(@RequestParam(required = false) String token,
                                   @RequestParam(required = false) String authorization,
                                   ServerWebExchange webExchange) {
        // queries是url参数
        Map<String, String> queries = new HashMap<String, String>() {{
            put("param0", "value0");
            put("param1", "value1");
        }};
        return defaultNoBody(UriMaps.Test_GET, queries, handleAuth(token, authorization), webExchange);
    }

    /**
     * 如果使用@RequestBody，框架会消费掉ServerWebExchange当中的内容
     *
     *
     */
    @PostMapping("/test/post")
    public Mono<String> postTest_01(@RequestParam(required = false) String token,
                                    @RequestParam(required = false) String authorization,
                                    ServerWebExchange webExchange) {
        // queries是url参数
        Map<String, String> queries = new HashMap<String, String>() {{
            put("param0", "value0");
            put("param1", "value1");
        }};
        return defaultWithBody(UriMaps.Test_POST_WithBody, queries, handleAuth(token, authorization), webExchange);
    }

}
