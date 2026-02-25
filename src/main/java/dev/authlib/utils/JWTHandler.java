package dev.authlib.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.authlib.config.Config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token handling utility
 */

public class JWTHandler {
    private final Config config;
    private final Algorithm algorithm;

    public JWTHandler(Config config) {
        this.config = config;
        this.algorithm = Algorithm.HMAC256(config.JWT_SECRET_KEY);
    }

    /**
     * Create an access token
     */
    public String createAccessToken(int userId, String email, Map<String, ?> additionalClaims) {
        if (userId <= 0) {
            throw new ValidationError("userId must be a positive number");
        }
        if (email == null || email.isEmpty()) {
            throw new ValidationError("email must not be empty");
        }

        Instant expiresAt = Instant.now()
            .plus(config.JWT_ACCESS_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        var builder = JWT.create()
            .withJWTId(UUID.randomUUID().toString())
            .withIssuer("authlib")
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(expiresAt))
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("type", "access");

        if (additionalClaims != null) {
            for (Map.Entry<String, ?> entry : additionalClaims.entrySet()) {
                if (entry.getValue() instanceof String) {
                    builder.withClaim(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    builder.withClaim(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    builder.withClaim(entry.getKey(), (Boolean) entry.getValue());
                }
            }
        }

        return builder.sign(algorithm);
    }

    /**
     * Create a refresh token
     */
    public String createRefreshToken(int userId, String email, Map<String, ?> additionalClaims) {
        if (userId <= 0) {
            throw new ValidationError("userId must be a positive number");
        }
        if (email == null || email.isEmpty()) {
            throw new ValidationError("email must not be empty");
        }

        Instant expiresAt = Instant.now()
            .plus(config.JWT_REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS);

        var builder = JWT.create()
            .withJWTId(UUID.randomUUID().toString())
            .withIssuer("authlib")
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(expiresAt))
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("type", "refresh");

        if (additionalClaims != null) {
            for (Map.Entry<String, ?> entry : additionalClaims.entrySet()) {
                if (entry.getValue() instanceof String) {
                    builder.withClaim(entry.getKey(), (String) entry.getValue());
                }
            }
        }

        return builder.sign(algorithm);
    }

    /**
     * Verify and decode a token
     */
    public TokenPayload verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("authlib")
                .build();

            DecodedJWT decodedJWT = verifier.verify(token);
            return parsePayload(decodedJWT);
        } catch (JWTVerificationException e) {
            throw new InvalidToken("Token verification failed: " + e.getMessage());
        }
    }

    /**
     * Decode token without verification
     */
    public TokenPayload decodeToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return parsePayload(decodedJWT);
        } catch (Exception e) {
            throw new InvalidToken("Failed to decode token");
        }
    }

    private TokenPayload parsePayload(DecodedJWT decodedJWT) {
        return new TokenPayload(
            decodedJWT.getClaim("userId").asInt(),
            decodedJWT.getClaim("email").asString(),
            decodedJWT.getClaim("type").asString(),
            decodedJWT.getIssuedAt().getTime() / 1000,
            decodedJWT.getExpiresAt().getTime() / 1000
        );
    }

    /**
     * Token payload structure
     */
    public static class TokenPayload {
        private final int userId;
        private final String email;
        private final String type;
        private final long iat;
        private final long exp;

        public TokenPayload(int userId, String email, String type, long iat, long exp) {
            this.userId = userId;
            this.email = email;
            this.type = type;
            this.iat = iat;
            this.exp = exp;
        }

        public int getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getType() { return type; }
        public long getIssuedAt() { return iat; }
        public long getExpiresAt() { return exp; }
    }
}
