package com.mumuca.moneytracker.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuca.moneytracker.api.exception.dto.APIErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String jwtFromCookie = request.getCookies() != null ? Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("jwt"))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null) : null;

        if (jwtFromCookie == null) {
            filterChain.doFilter(request, response);
            return;
        }

        request = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) {
                    return "Bearer " + jwtFromCookie;
                }
                return super.getHeader(name);
            }
        };

        try {
            System.out.println("Validating JWT:" + jwtFromCookie);
            Jwt jwt = jwtDecoder.decode(jwtFromCookie);

            Authentication authentication = createBearerAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException ex) {
            ex.printStackTrace();
            APIErrorResponse<String> errorResponse = new APIErrorResponse<String>(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    LocalDateTime.now(),
                    "Invalid or expired token",
                    "The provided token is invalid or expired. Please sign in again."
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Authentication createBearerAuthentication(Jwt jwt) {
        String userId = jwt.getSubject();

        List<String> roles = jwt
                .getClaimAsStringList("roles");

        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());

        Map<String, Object> attributes = Map.of(
                "sub", userId,
                "roles", roles
        );

        OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                userId,
                attributes,
                authorities
        );

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt()
        );

        return new BearerTokenAuthentication(principal, accessToken, authorities);
    }
}
