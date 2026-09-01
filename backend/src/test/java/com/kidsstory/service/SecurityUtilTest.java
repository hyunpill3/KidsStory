package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kidsstory.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityUtilTest {

    private SecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setSecretKey("test-secret-key");
        appProperties.setAccessTokenExpireMinutes(60);
        securityUtil = new SecurityUtil(appProperties);
    }

    @Test
    void hashedPasswordVerifiesAgainstTheOriginalPlaintext() {
        String hash = securityUtil.hashPassword("correct horse battery staple");

        assertThat(hash).isNotEqualTo("correct horse battery staple");
        assertThat(securityUtil.verifyPassword("correct horse battery staple", hash)).isTrue();
        assertThat(securityUtil.verifyPassword("wrong password", hash)).isFalse();
    }

    @Test
    void accessTokenRoundTripsBackToItsSubject() {
        String token = securityUtil.createAccessToken("user-123");

        assertThat(securityUtil.decodeAccessToken(token)).isEqualTo("user-123");
    }

    @Test
    void decodingAGarbageTokenReturnsNullRatherThanThrowing() {
        assertThat(securityUtil.decodeAccessToken("not-a-real-jwt")).isNull();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        String token = securityUtil.createAccessToken("user-123");

        AppProperties otherProperties = new AppProperties();
        otherProperties.setSecretKey("a-completely-different-secret");
        otherProperties.setAccessTokenExpireMinutes(60);
        SecurityUtil otherSecurityUtil = new SecurityUtil(otherProperties);

        assertThat(otherSecurityUtil.decodeAccessToken(token)).isNull();
    }

    @Test
    void shortSecretIsStillAcceptedViaPadding() {
        // A short SECRET_KEY (like the dev default) must not blow up HS256's
        // 256-bit minimum key length requirement.
        AppProperties shortKeyProperties = new AppProperties();
        shortKeyProperties.setSecretKey("short");
        shortKeyProperties.setAccessTokenExpireMinutes(60);
        SecurityUtil shortKeySecurityUtil = new SecurityUtil(shortKeyProperties);

        String token = shortKeySecurityUtil.createAccessToken("user-123");

        assertThat(shortKeySecurityUtil.decodeAccessToken(token)).isEqualTo("user-123");
    }
}
