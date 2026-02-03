package com.pgsa.trailers.service.security;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final Key signingKey;

    private static final long EXPIRY = 1000 * 60 * 60 * 8; // 8 hours

    /**
     * Generate a JWT token with userId as subject and email + authorities as claims.
     */
    public String generateToken(UserDetails userDetails, Long userId, String email) {
        // Ensure ROLE_ prefix
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toList());

        Map<String, Object> claims = Map.of(
                "authorities", roles,
                "email", email
        );

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }


    public Long extractUserId(String token) {
        return Long.valueOf(extractClaim(token, Claims::getSubject));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> claims.get("authorities", List.class));
    }

    public String extractUsername(String token) {
        return extractEmail(token);
    }

    public boolean isValid(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parse(token).getBody();
        return claimsResolver.apply(claims);
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }
}