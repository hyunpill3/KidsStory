package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Verifies a Cloudflare Turnstile token. Captcha is skipped (always passes)
 * when no secret key is configured, so local/dev environments work without
 * setting up Turnstile.
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final AppProperties appProperties;
    private final RestClient restClient = RestClient.create();

    public boolean verifyCaptcha(String token, String clientIp) {
        if (appProperties.getTurnstileSecretKey().isBlank()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", appProperties.getTurnstileSecretKey());
            form.add("response", token);
            form.add("remoteip", clientIp != null ? clientIp : "");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(TURNSTILE_VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception ex) {
            return false;
        }
    }
}
