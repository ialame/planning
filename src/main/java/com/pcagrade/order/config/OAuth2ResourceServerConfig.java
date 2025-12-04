package com.pcagrade.order.config;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * OAuth2 Resource Server configuration for Authentik JWT validation.
 *
 * This configuration is DISABLED when security.oauth2.disabled=true
 * (used for Docker development without Authentik access)
 */
@Configuration
@ConditionalOnProperty(name = "security.oauth2.disabled", havingValue = "false", matchIfMissing = true)
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