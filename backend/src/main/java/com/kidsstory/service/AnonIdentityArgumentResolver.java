package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves an {@link AnonIdentity} controller parameter, mirroring FastAPI's
 * get_anon_identity dependency: reads the anon-id cookie (setting it on first
 * visit) and the client IP, with no account/login required.
 */
@Component
@RequiredArgsConstructor
public class AnonIdentityArgumentResolver implements HandlerMethodArgumentResolver {

    private final AppProperties appProperties;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(AnonIdentity.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        String anonId = readCookie(request, appProperties.getAnonCookieName());
        if (anonId == null) {
            anonId = UUID.randomUUID().toString().replace("-", "");
            setCookie(webRequest, anonId);
        }

        return new AnonIdentity(anonId, resolveClientIp(request));
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    private void setCookie(NativeWebRequest webRequest, String anonId) {
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) return;

        ResponseCookie cookie = ResponseCookie.from(appProperties.getAnonCookieName(), anonId)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(!"local".equals(appProperties.getEnvironment()))
                .maxAge(Duration.ofDays(appProperties.getAnonCookieMaxAgeDays()))
                .path("/")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
