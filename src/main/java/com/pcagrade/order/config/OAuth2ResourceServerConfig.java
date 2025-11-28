package com.pcagrade.order.config;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@Configuration
public class OAuth2ResourceServerConfig {

    @Value("${authentik.jwks}")
    private String jwksJson;

    @Bean
    public JwtDecoder jwtDecoder() throws ParseException, JOSEException {
        JWKSet jwkSet = JWKSet.parse(jwksJson);

        // Get the first RSA key
        JWK jwk = jwkSet.getKeys().get(0);
        RSAPublicKey publicKey = ((RSAKey) jwk).toRSAPublicKey();

        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}