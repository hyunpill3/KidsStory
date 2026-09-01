package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kidsstory.config.AppProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;

class AnonIdentityArgumentResolverTest {

    private AppProperties appProperties;
    private AnonIdentityArgumentResolver resolver;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setAnonCookieName("ks_anon_id");
        appProperties.setAnonCookieMaxAgeDays(30);
        appProperties.setEnvironment("local");
        resolver = new AnonIdentityArgumentResolver(appProperties);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class)).thenReturn(request);
        when(webRequest.getNativeResponse(jakarta.servlet.http.HttpServletResponse.class)).thenReturn(response);
    }

    @Test
    void supportsOnlyTheAnonIdentityParameterType() throws NoSuchMethodException {
        var anonIdentityParam = new org.springframework.core.MethodParameter(
                getClass().getDeclaredMethod("dummyWithAnonIdentity", AnonIdentity.class), 0);
        var stringParam = new org.springframework.core.MethodParameter(
                getClass().getDeclaredMethod("dummyWithString", String.class), 0);

        assertThat(resolver.supportsParameter(anonIdentityParam)).isTrue();
        assertThat(resolver.supportsParameter(stringParam)).isFalse();
    }

    // Referenced via reflection above only to obtain a real MethodParameter for each type.
    @SuppressWarnings("unused")
    private void dummyWithAnonIdentity(AnonIdentity identity) {
    }

    @SuppressWarnings("unused")
    private void dummyWithString(String value) {
    }

    @Test
    void reusesAnExistingAnonCookieRatherThanIssuingANewOne() throws Exception {
        request.setCookies(new Cookie("ks_anon_id", "existing-anon-id"));

        AnonIdentity identity = (AnonIdentity) resolver.resolveArgument(null, null, webRequest, null);

        assertThat(identity.anonId()).isEqualTo("existing-anon-id");
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @Test
    void issuesANewAnonCookieWhenNoneIsPresent() throws Exception {
        AnonIdentity identity = (AnonIdentity) resolver.resolveArgument(null, null, webRequest, null);

        assertThat(identity.anonId()).isNotBlank();
        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("ks_anon_id=" + identity.anonId());
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).containsIgnoringCase("SameSite=Lax");
    }

    @Test
    void prefersXForwardedForOverRemoteAddrWhenResolvingClientIp() throws Exception {
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");

        AnonIdentity identity = (AnonIdentity) resolver.resolveArgument(null, null, webRequest, null);

        assertThat(identity.clientIp()).isEqualTo("203.0.113.5");
    }

    @Test
    void fallsBackToRemoteAddrWhenNoForwardedForHeaderIsPresent() throws Exception {
        request.setRemoteAddr("192.168.1.50");

        AnonIdentity identity = (AnonIdentity) resolver.resolveArgument(null, null, webRequest, null);

        assertThat(identity.clientIp()).isEqualTo("192.168.1.50");
    }
}
