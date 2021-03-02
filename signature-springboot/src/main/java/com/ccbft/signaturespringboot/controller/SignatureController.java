package com.ccbft.signaturespringboot.controller;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
@RequestMapping("/common")
public class SignatureController {

    private static final String host = "http://103.126.126.119:8002";

    @Getter
    public enum UriMaps {
        Test_GET_00(HttpMethod.GET, "/phjr/bds/signature/tester");

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

    private Mono<String> defaultGet(UriMaps uriMaps, String token, String authorization, ServerWebExchange webExchange){
        Assert.isTrue(HttpMethod.GET.equals(uriMaps.getHttpMethod()), "method must be GET");
        String[] auths = handleAuth(token, authorization);

        return WebClient
                .create(host)
                .get()
                .uri(uriMaps.getRoute())
                .header(auths[0], auths[1])
                .exchangeToMono(clientResponse -> {
                    webExchange.getResponse().setStatusCode(clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class);
                });
    }


    @GetMapping("/test/get")
    public Mono<String> getTest_01(@RequestParam(required = false) String token, @RequestParam(required = false) String authorization, ServerWebExchange webExchange) {
        return defaultGet(UriMaps.Test_GET_00, token, authorization, webExchange);
    }
}
