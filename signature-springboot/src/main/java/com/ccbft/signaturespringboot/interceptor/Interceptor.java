package com.ccbft.signaturespringboot.interceptor;

import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;

public class Interceptor extends ClientHttpRequestDecorator {



    public Interceptor(ClientHttpRequest delegate) {
        super(delegate);
    }
}
