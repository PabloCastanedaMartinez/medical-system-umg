package com.cecamed.backend.api.security;

import com.cecamed.backend.application.identity.model.TokenClaims;
import com.cecamed.backend.application.identity.port.out.TokenValidatorPort;
import com.cecamed.backend.domain.identity.exception.InvalidTokenException;
import com.cecamed.backend.domain.identity.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenValidatorPort tokenValidator;
    private final JsonMapper jsonMapper;

    public JwtAuthenticationFilter(TokenValidatorPort tokenValidator, JsonMapper jsonMapper) {
        this.tokenValidator = Objects.requireNonNull(tokenValidator);
        this.jsonMapper = Objects.requireNonNull(jsonMapper);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            TokenClaims claims = tokenValidator.validateAccessToken(token);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.username(),
                    null,
                    authorities
            );
            authentication.setDetails(claims);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (TokenExpiredException e) {
            SecurityContextHolder.clearContext();
            writeProblem(response, request.getRequestURI(), HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", e.getMessage());
            return;
        } catch (InvalidTokenException e) {
            SecurityContextHolder.clearContext();
            writeProblem(response, request.getRequestURI(), HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", e.getMessage());
            return;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            writeProblem(response, request.getRequestURI(), HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Error al procesar la autenticación.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeProblem(
            HttpServletResponse response,
            String uri,
            HttpStatus status,
            String code,
            String detail) throws IOException {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        problem.setInstance(URI.create(uri));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
