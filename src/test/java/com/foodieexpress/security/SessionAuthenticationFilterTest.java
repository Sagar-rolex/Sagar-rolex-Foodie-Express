package com.foodieexpress.security;

import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidSessionHeader() throws Exception {
        AuthService authService = mock(AuthService.class);
        User admin = User.builder().id(9L).username("admin").email("admin@foodie.com").role(User.Role.ADMIN).build();
        when(authService.validateSession("admin-token")).thenReturn(Optional.of(admin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionAuthenticationFilter.SESSION_HEADER, "admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new SessionAuthenticationFilter(authService).doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(((SessionPrincipal) authentication.getPrincipal()).username()).isEqualTo("admin");
        verify(chain).doFilter(eq(request), eq(response));
    }

    @Test
    void ignoresMissingOrInvalidTokens() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.validateSession(null)).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new SessionAuthenticationFilter(authService).doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(eq(request), eq(response));
        verify(authService).validateSession(null);
    }

    @Test
    void acceptsBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc-123");
        assertThat(SessionAuthenticationFilter.resolveToken(request)).isEqualTo("abc-123");
    }

    @Test
    void bearerTokenIsAvailableToControllersUsingLegacyHeader() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.validateSession("abc-123")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        java.util.concurrent.atomic.AtomicReference<jakarta.servlet.ServletRequest> forwarded =
                new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(invocation -> {
            forwarded.set(invocation.getArgument(0));
            return null;
        }).when(chain).doFilter(any(), eq(response));

        new SessionAuthenticationFilter(authService).doFilter(request, response, chain);

        assertThat(((jakarta.servlet.http.HttpServletRequest) forwarded.get())
                .getHeader(SessionAuthenticationFilter.SESSION_HEADER)).isEqualTo("abc-123");
        verify(authService).validateSession("abc-123");
    }
}
