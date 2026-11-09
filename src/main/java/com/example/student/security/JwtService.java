package com.example.student.security;

import com.example.student.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            "modernprogrammingtechnologiesisinterestingandusefullstudentsloveitreadwritecodeenjoyaprogrammerslifeletusnotgetboredwithrestap".getBytes()
    );
    public String generateToken(String username, Set<Role> roles) {
        List<String> roleNames = roles.stream()
                .map(Enum::name)
                .toList();
        return Jwts.builder()
                .subject(username)
                .claim("roles", roleNames)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 час
                .signWith(SECRET_KEY)
                .compact();
    }
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    @SuppressWarnings("unchecked")
    public Set<Role> extractRoles(String token) {
        List<String> roleNames = parseClaims(token).get("roles", List.class);
        return roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }
}