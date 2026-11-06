package com.foodieexpress.security;

import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER = "X-Session-Token";

    private final AuthService authService;

    public SessionAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest authenticatedRequest = requestWithSessionHeader(request);
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authService.validateSession(resolveToken(authenticatedRequest)).ifPresent(user ->
                    SecurityContextHolder.getContext().setAuthentication(authenticationFor(user, authenticatedRequest)));
        }
        filterChain.doFilter(authenticatedRequest, response);
    }

    private HttpServletRequest requestWithSessionHeader(HttpServletRequest request) {
        String headerToken = request.getHeader(SESSION_HEADER);
        String bearerToken = resolveBearerToken(request);
        if ((headerToken != null && !headerToken.isBlank()) || bearerToken == null) {
            return request;
        }
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (SESSION_HEADER.equalsIgnoreCase(name)) {
                    return bearerToken;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (SESSION_HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(bearerToken));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                if (names.stream().noneMatch(SESSION_HEADER::equalsIgnoreCase)) {
                    names = new ArrayList<>(names);
                    names.add(SESSION_HEADER);
                }
                return Collections.enumeration(names);
            }
        };
    }

    private UsernamePasswordAuthenticationToken authenticationFor(User user, HttpServletRequest request) {
        SessionPrincipal principal = new SessionPrincipal(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    static String resolveToken(HttpServletRequest request) {
        String headerToken = request.getHeader(SESSION_HEADER);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }
        return resolveBearerToken(request);
    }

    private static String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearer = authorization.substring(7).trim();
            return bearer.isEmpty() ? null : bearer;
        }
        return null;
    }
}
