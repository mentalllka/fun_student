package com.example.student.security;

import com.example.student.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private final JwtService service = new JwtService();

    @Test
    void tokenContainsUsernameAllRolesAndOneHourLifetime() {
        String token = service.generateToken("user@example.com", Set.of(Role.ADMIN, Role.TEACHER));
        assertThat(service.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(service.extractRoles(token)).containsExactlyInAnyOrder(Role.ADMIN, Role.TEACHER);
        assertThat(service.validateToken(token, User.withUsername("user@example.com")
                .password("hash").roles("ADMIN").build())).isTrue();
        // Подпись и срок проверяются тем же ключом, которым пользуется исходный сервис.
        Claims claims = Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isBetween(3_600_000L, 3_601_000L);
    }

    @Test
    void tokenForAnotherUserIsNotValid() {
        String token = service.generateToken("one@example.com", Set.of(Role.TEACHER));
        assertThat(service.validateToken(token, User.withUsername("two@example.com")
                .password("hash").roles("TEACHER").build())).isFalse();
    }

    @Test
    void emptyRolesArePreserved() {
        String token = service.generateToken("user@example.com", Set.of());
        assertThat(service.extractRoles(token)).isEmpty();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> service.extractUsername("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejectedWithoutSleeping() {
        String token = Jwts.builder().subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() - 60_000)).signWith(signingKey()).compact();
        assertThatThrownBy(() -> service.validateToken(token, User.withUsername("user@example.com")
                .password("hash").roles("ADMIN").build())).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedByAnotherKeyIsRejected() {
        String token = Jwts.builder().subject("user@example.com")
                .signWith(Jwts.SIG.HS512.key().build()).compact();
        assertThatThrownBy(() -> service.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    private javax.crypto.SecretKey signingKey() {
        return Keys.hmacShaKeyFor(("modernprogrammingtechnologiesisinterestingandusefullstudentsloveitreadwritecodeenjoyaprogrammerslifeletusnotgetboredwithrestap")
                .getBytes(StandardCharsets.UTF_8));
    }
}
