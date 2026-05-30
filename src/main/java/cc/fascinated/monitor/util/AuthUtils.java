package cc.fascinated.monitor.util;

import cc.fascinated.monitor.exception.impl.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public class AuthUtils {
    /**
     * Parses a Bearer token into the UUID from it.
     *
     * @param authorizationHeader the header to use
     * @return the auth token
     */
    public static String extractBearerValue(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return token;
    }

    public static UUID parseBearerToken(String authorizationHeader) {
        try {
            return UUID.fromString(extractBearerValue(authorizationHeader));
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid ingest token");
        }
    }

    /**
     * Hashes a value with SHA-256 for storage or comparison.
     *
     * @param value the value to hash
     * @return the lowercase hex-encoded hash
     */
    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Checks whether a value matches a previously stored hash.
     *
     * @param value the raw value to check
     * @param hash  the stored hash
     * @return true if the value matches the hash
     */
    public static boolean matchesHash(String value, String hash) {
        return MessageDigest.isEqual(
                hash(value).getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Constant-time comparison of a configured bearer token against an Authorization header.
     *
     * @param expected            the expected bearer token value
     * @param authorizationHeader the Authorization header, or null
     * @return true if the bearer token matches
     */
    public static boolean bearerTokensEqual(String expected, String authorizationHeader) {
        if (expected == null || expected.isEmpty()) {
            return false;
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        );
    }
}
