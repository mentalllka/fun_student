package com.example.student.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {
    @Mock private UserDetailsService detailsService;
    @Mock private JwtService jwtService;
    @Mock private FilterChain chain;
    private JwtFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtFilter(detailsService, jwtService);
        request = new MockHttpServletRequest("GET", "/api/v1/student");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void noHeaderContinuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, detailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void otherAuthorizationSchemeIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic abc");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, detailsService);
    }

    @Test
    void validTokenAuthenticatesUserWithRoles() throws Exception {
        request.addHeader("Authorization", "Bearer valid");
        var details = User.withUsername("user@example.com").password("hash").roles("ADMIN").build();
        when(jwtService.extractUsername("valid")).thenReturn("user@example.com");
        when(detailsService.loadUserByUsername("user@example.com")).thenReturn(details);
        when(jwtService.validateToken("valid", details)).thenReturn(true);
        filter.doFilter(request, response, chain);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo("user@example.com");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        verify(chain).doFilter(request, response);
    }

    @Test
    void failedValidationDoesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer invalid");
        var details = User.withUsername("user@example.com").password("hash").roles("ADMIN").build();
        when(jwtService.extractUsername("invalid")).thenReturn("user@example.com");
        when(detailsService.loadUserByUsername("user@example.com")).thenReturn(details);
        when(jwtService.validateToken("invalid", details)).thenReturn(false);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void nullSubjectDoesNotLoadUser() throws Exception {
        request.addHeader("Authorization", "Bearer no-subject");
        when(jwtService.extractUsername("no-subject")).thenReturn(null);
        filter.doFilter(request, response, chain);
        verifyNoInteractions(detailsService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void existingAuthenticationIsNotReplaced() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken("existing", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer valid");
        when(jwtService.extractUsername("valid")).thenReturn("other@example.com");
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(detailsService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidApiTokenReturns401AndStopsChain() throws Exception {
        request.addHeader("Authorization", "Bearer broken");
        when(jwtService.extractUsername("broken")).thenThrow(new IllegalArgumentException("bad token"));
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("bad token", "IllegalArgumentException");
        verifyNoInteractions(chain);
    }

    @Test
    void missingTokenUserReturns401() throws Exception {
        request.addHeader("Authorization", "Bearer valid");
        when(jwtService.extractUsername("valid")).thenReturn("deleted@example.com");
        when(detailsService.loadUserByUsername("deleted@example.com"))
                .thenThrow(new UsernameNotFoundException("not found"));
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void nonApiTokenErrorIsRethrown() {
        request.setRequestURI("/index.html");
        request.addHeader("Authorization", "Bearer broken");
        when(jwtService.extractUsername("broken")).thenThrow(new IllegalArgumentException("bad token"));
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("bad token");
        verifyNoInteractions(chain);
    }
}
