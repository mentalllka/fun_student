package com.example.student.service;

import com.example.student.model.*;
import com.example.student.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(repository);

    @Test
    void loadsAllRolesWithSecurityPrefix() {
        User u = User.builder().email("user@example.com").password("hash")
                .roles(Set.of(Role.ADMIN, Role.TEACHER)).build();
        when(repository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        var details = service.loadUserByUsername(u.getEmail());
        assertThat(details.getUsername()).isEqualTo(u.getEmail());
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_TEACHER");
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void missingUserThrowsSecurityException() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class).hasMessageContaining("missing@example.com");
    }
}
