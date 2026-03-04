package com.ASP_Project.Identity_service.Service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    public static final String SECRET_KEY = "4496b750c04e9f53dc56118babf69f00";


    public void validateToken(final String token) {
        Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
    }

    public String generateToken(String userName, String role, Long id, String name, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("id", id);
        claims.put("name", name);
        claims.put("email", email);
        return createToken(claims, userName);
    }

    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Token valid for 10 hours
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // Use HS256 for HMAC key
                .compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(); // Use raw bytes if not base64 encoded
        return Keys.hmacShaKeyFor(keyBytes);
    }

//    public String extractRole(String token) {
//        return Jwts.parserBuilder()
//            .setSigningKey(getSignKey())
//            .build()
//            .parseClaimsJws(token)
//            .getBody()
//            .get("role", String.class);
//    }
//
//    public String extractId(String token) {
//        return Jwts.parserBuilder()
//            .setSigningKey(getSignKey())
//            .build()
//            .parseClaimsJws(token)
//            .getBody()
//            .get("id", String.class);
//    }
//
//    public String extractName(String token) {
//        return Jwts.parserBuilder()
//            .setSigningKey(getSignKey())
//            .build()
//            .parseClaimsJws(token)
//            .getBody()
//            .get("name", String.class);
//    }
//
//    public String extractEmail(String token) {
//        return Jwts.parserBuilder()
//            .setSigningKey(getSignKey())
//            .build()
//            .parseClaimsJws(token)
//            .getBody()
//            .get("email", String.class);
//    }
//
//    public String extractTokenFromHeader(String header) {
//        if (header != null && header.startsWith("Bearer ")) {
//            return header.substring(7);
//        }
//        return null;
//    }
}
