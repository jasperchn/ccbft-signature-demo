package com.ccbft.signaturespringboot.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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
        Test_GET_00(HttpMethod.GET, "/bds/signature/tester");

        private HttpMethod httpMethod;
        private String route;

        UriMaps(HttpMethod httpMethod, String route) {
            this.httpMethod = httpMethod;
            this.route = route;
        }
    }

    private String[] handleAuth(String token, String authorization) {
        Assert.isTrue(StringUtils.hasText(token) ^ StringUtils.hasText(authorization), "provide either token or authorization");
        if(StringUtils.hasText(token))
            return new String[]{"token", "Bearer " + token};
        else
            return new String[]{"Authorization", "Bearer " + authorization};
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

    private Mono<String> defaultGet(UriMaps uriMaps, Map<String, String> queries, String token, String authorization, ServerWebExchange webExchange){
        Assert.isTrue(HttpMethod.GET.equals(uriMaps.getHttpMethod()), "method must be GET");
        String[] auths = handleAuth(token, authorization);

        /**
         * 计算签名
         * */
        String timestamp = SignatureUtils.generateTimestamp(Config.TIMESTAMP_BIAS);
        String nonce = SignatureUtils.generateNonce(Config.NONCE_LENGTH);
        String signature = SignatureUtils.buildSignature(
                HttpMethod.GET.name(),
                uriMaps.getRoute(),
                "",
                "",
                timestamp,
                nonce,
                Config.appKey,
                Config.appSecret,
                Collections.emptyMap(),
                queries
        );
        log.info("signature = {}", signature);

        /**
         * 放置Header
         * */
        Consumer<HttpHeaders> headersConsumer = httpHeaders -> {
            httpHeaders.add(auths[0], auths[1]);
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

    /**
     * token为短token
     * authorization为长token
     * 二选一即可
     * */
    @GetMapping("/test/get")
    public Mono<String> getTest_01(@RequestParam(required = false) String token, @RequestParam(required = false) String authorization, ServerWebExchange webExchange) {
        // queries是url参数
        Map<String, String> queries = new HashMap<String, String>() {{
            put("param0", "value0");
            put("param1", "value1");
        }};
        return defaultGet(UriMaps.Test_GET_00, queries, token, authorization, webExchange);
    }
}
