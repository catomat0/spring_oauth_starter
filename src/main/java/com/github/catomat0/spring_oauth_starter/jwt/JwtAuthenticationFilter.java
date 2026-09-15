package com.github.catomat0.spring_oauth_starter.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Authorization: Bearer &lt;access_token&gt; 헤더에서 access token 을 추출하여 SecurityContext 에 인증정보를 세팅한다.
 * <p>SecurityFilterChain 에서 {@code UsernamePasswordAuthenticationFilter} 앞에 등록.
 * <p>토큰이 없거나 검증 실패 시 chain 을 통과시켜 후속 Spring Security 가 401 처리.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && jwtProvider.validateAccess(token)) {
            try {
                JwtPayload payload = jwtProvider.parseAccess(token);
                List<SimpleGrantedAuthority> authorities = payload.role() != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + payload.role()))
                        : Collections.emptyList();
                var auth = new UsernamePasswordAuthenticationToken(
                        payload.userId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                // parse 실패 → 인증정보 세팅 없이 통과 → Spring Security 가 401 처리
            }
        }
        chain.doFilter(request, response);
    }

    private static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
