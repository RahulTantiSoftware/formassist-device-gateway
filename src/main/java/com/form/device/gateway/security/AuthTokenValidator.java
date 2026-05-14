package com.form.device.gateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;

@Component
public class AuthTokenValidator {

    private final JwksService jwksService;

    public AuthTokenValidator(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    public JWTClaimsSet validate(String token) throws Exception {

        SignedJWT signedJWT = SignedJWT.parse(token);

        if (!JWSAlgorithm.ES256.equals(signedJWT.getHeader().getAlgorithm())) {
            throw new SecurityException("Unsupported JWT algorithm");
        }

        String kid = signedJWT.getHeader().getKeyID();

        JWK jwk = (kid != null)
                ? jwksService.getJwkSet().getKeyByKeyId(kid)
                : jwksService.getJwkSet().getKeys().getFirst();

        if (!(jwk instanceof  ECKey)) {
            throw new SecurityException("Invalid EC JWK");
        }

        ECKey ecKey = (ECKey) jwk;

        JWSVerifier verifier =
                new ECDSAVerifier(
                        ecKey.toECPublicKey()
                );

        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Invalid JWT signature");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        Date exp = claims.getExpirationTime();
        if (exp == null) {
            throw new IllegalArgumentException("token missing exp");
        }
        if (exp.before(new Date())) {
            throw new IllegalArgumentException("token expired");
        }

        return claims;
    }


    public boolean hasRole(JWTClaimsSet claims, String role) {
        Set<String> roles = (Set<String>) claims.getClaim("roles");
        return roles != null && roles.contains(role);
    }
}
