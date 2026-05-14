package com.form.device.gateway.security;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class JwksService {
    private  final String authUrl;
    private final AtomicReference<JWKSet> cachedKeys = new AtomicReference<>();

    public JwksService(@Value("${auth-service.url}")String authUrl) throws Exception {
        this.authUrl = authUrl + "/auth/keys";
        refreshKeys();
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void refreshKeys() throws Exception {
        cachedKeys.set(JWKSet.load(URI.create(authUrl).toURL()));
        System.out.println("JWKS refreshed from AuthService");
    }

    public JWKSet getJwkSet() {
        return cachedKeys.get();
    }

}
