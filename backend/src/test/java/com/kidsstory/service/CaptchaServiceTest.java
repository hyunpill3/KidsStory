package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kidsstory.config.AppProperties;
import org.junit.jupiter.api.Test;

class CaptchaServiceTest {

    @Test
    void skipsVerificationWhenNoSecretKeyIsConfigured() {
        AppProperties appProperties = new AppProperties();
        appProperties.setTurnstileSecretKey("");
        CaptchaService captchaService = new CaptchaService(appProperties);

        // No network call is made in this branch - a null/blank token still passes,
        // which is exactly what lets local/dev environments skip Turnstile setup.
        assertThat(captchaService.verifyCaptcha(null, "127.0.0.1")).isTrue();
    }

    @Test
    void rejectsBlankTokenWhenSecretKeyIsConfigured() {
        AppProperties appProperties = new AppProperties();
        appProperties.setTurnstileSecretKey("configured-secret");
        CaptchaService captchaService = new CaptchaService(appProperties);

        assertThat(captchaService.verifyCaptcha(null, "127.0.0.1")).isFalse();
        assertThat(captchaService.verifyCaptcha("   ", "127.0.0.1")).isFalse();
    }
}
