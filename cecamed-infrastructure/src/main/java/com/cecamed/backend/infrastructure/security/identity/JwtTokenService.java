package com.cecamed.backend.infrastructure.security.identity;

import com.cecamed.backend.application.identity.model.TokenClaims;
import com.cecamed.backend.application.identity.port.out.TokenIssuerPort;
import com.cecamed.backend.application.identity.port.out.TokenValidatorPort;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.InvalidTokenException;
import com.cecamed.backend.domain.identity.exception.TokenExpiredException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JwtTokenService implements TokenIssuerPort, TokenValidatorPort {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String ISSUER = "cecamed-backend";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_BASE64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private final byte[] secretKeyBytes;
    private final int accessTokenExpirationMinutes;
    private final int refreshTokenExpirationDays;
    private final Clock clock;
    private final ObjectMapper jsonMapper;
    private final SecureRandom secureRandom;

    public JwtTokenService(
            @Value("${cecamed.security.jwt.secret:change-this-jwt-secret-key-in-production-at-least-256-bits!}") String secret,
            @Value("${cecamed.security.jwt.access-token-expiration-minutes:15}") int accessTokenExpirationMinutes,
            @Value("${cecamed.security.jwt.refresh-token-expiration-days:7}") int refreshTokenExpirationDays,
            Clock clock,
            ObjectMapper jsonMapper) {
        Objects.requireNonNull(secret, "secret no puede ser nulo");
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("La clave secreta JWT debe tener al menos 256 bits (32 bytes).");
        }
        this.secretKeyBytes = bytes;
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.clock = Objects.requireNonNull(clock);
        this.jsonMapper = Objects.requireNonNull(jsonMapper);
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String issueAccessToken(UserAccount user, Instant now) {
        Instant expiresAt = now.plusSeconds(accessTokenExpirationMinutes * 60L);

        Map<String, Object> payload = Map.of(
                "sub", user.getUsername(),
                "userId", user.getId(),
                "role", user.getRole().name(),
                "fullName", user.getFullName(),
                "iss", ISSUER,
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        );

        String payloadJson;
        try {
            payloadJson = jsonMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar claims de JWT", e);
        }

        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String contentToSign = HEADER_BASE64 + "." + payloadBase64;
        String signatureBase64 = sign(contentToSign);

        return contentToSign + "." + signatureBase64;
    }

    @Override
    public TokenClaims validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("El token no puede ser nulo ni vacío.");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Formato de token JWT inválido.");
        }

        String headerBase64 = parts[0];
        String payloadBase64 = parts[1];
        String signatureBase64 = parts[2];

        String contentToSign = headerBase64 + "." + payloadBase64;
        String expectedSignature = sign(contentToSign);

        if (!MessageDigest.isEqual(
                signatureBase64.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidTokenException("Firma del token no válida.");
        }

        Map<String, Object> claimsMap;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            claimsMap = jsonMapper.readValue(payloadBytes, Map.class);
        } catch (Exception e) {
            throw new InvalidTokenException("Contenido del token ilegible o corrupto.");
        }

        Object iss = claimsMap.get("iss");
        if (!ISSUER.equals(iss)) {
            throw new InvalidTokenException("Emisor de token inválido.");
        }

        Object expObj = claimsMap.get("exp");
        if (!(expObj instanceof Number expNumber)) {
            throw new InvalidTokenException("Fecha de expiración de token inválida.");
        }

        Instant expiresAt = Instant.ofEpochSecond(expNumber.longValue());
        Instant now = clock.instant();
        if (now.isAfter(expiresAt)) {
            throw new TokenExpiredException("El token de autenticación ha expirado.");
        }

        Object sub = claimsMap.get("sub");
        Object roleObj = claimsMap.get("role");
        Object fullNameObj = claimsMap.get("fullName");
        Object userIdObj = claimsMap.get("userId");
        Object iatObj = claimsMap.get("iat");

        if (sub == null || roleObj == null) {
            throw new InvalidTokenException("El token carece de claims obligatorios.");
        }

        Long userId = userIdObj instanceof Number n ? n.longValue() : null;
        Instant issuedAt = iatObj instanceof Number n ? Instant.ofEpochSecond(n.longValue()) : now;
        Role role;
        try {
            role = Role.valueOf(String.valueOf(roleObj));
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Rol inválido en el token: " + roleObj);
        }

        return new TokenClaims(
                userId,
                String.valueOf(sub),
                role,
                fullNameObj != null ? String.valueOf(fullNameObj) : "",
                issuedAt,
                expiresAt
        );
    }

    @Override
    public String issueRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("El token de actualización no puede estar vacío.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawRefreshToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }

    @Override
    public int getAccessTokenExpirationMinutes() {
        return accessTokenExpirationMinutes;
    }

    @Override
    public int getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKeyBytes, HMAC_SHA256));
            byte[] rawHmac = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular firma HMAC-SHA256", e);
        }
    }
}
