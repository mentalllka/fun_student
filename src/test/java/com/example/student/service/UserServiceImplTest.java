package com.example.student.service;

import com.example.student.dto.user.*;
import com.example.student.exception.*;
import com.example.student.mapper.UserMapper;
import com.example.student.mapper.UserMapperImpl;
import com.example.student.model.*;
import com.example.student.repository.UserRepository;
import com.example.student.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock private UserRepository repository;
    @Mock private PasswordEncoder encoder;
    private UserServiceImpl service;
    private User existing;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(repository, new UserMapperImpl(), encoder);
        existing = User.builder().id(5L).email("user@example.com").password("old-hash")
                .roles(Set.of(Role.TEACHER)).build();
    }

    private UserRequest request() {
        return new UserRequest("user@example.com", "secret", Set.of(Role.ADMIN));
    }

    @Test
    void existenceUsesEmailFromCaller() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        assertThat(service.existsByEmail("user@example.com")).isTrue();
        assertThat(service.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void registerHashesPasswordAndSavesFields() {
        when(encoder.encode("secret")).thenReturn("encoded");
        assertThat(service.register(request())).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(captor.getValue().getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void registerRejectsMissingEmailWithoutSaving() {
        assertThat(service.register(new UserRequest(null, "secret", Set.of(Role.TEACHER)))).isFalse();
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void registerRejectsNullEntityFromMapper() {
        UserMapper mapper = mock(UserMapper.class);
        UserServiceImpl serviceWithMapper = new UserServiceImpl(repository, mapper, encoder);
        UserRequest request = request();
        when(mapper.toEntity(request)).thenReturn(null);
        assertThat(serviceWithMapper.register(request)).isFalse();
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void registerRejectsDuplicateEmailWithoutSaving() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        assertThat(service.register(request())).isFalse();
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void registerDoesNotEncodeAbsentPassword(String password) {
        // Документируем ветвь исходного сервиса; HTTP-регистрации в проекте нет.
        assertThat(service.register(new UserRequest("user@example.com", password, Set.of(Role.TEACHER)))).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo(password);
        verifyNoInteractions(encoder);
    }

    @Test
    void findByEmailReturnsExistingEntity() {
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        assertThat(service.findByEmail("user@example.com")).isSameAs(existing);
    }

    @Test
    void findByEmailThrowsForMissingUser() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByEmail("missing@example.com"))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("missing@example.com");
    }

    @Test
    void findAllMapsEmailRolesAndCount() {
        when(repository.findAll()).thenReturn(List.of(existing));
        UserResponseList result = service.findAll();
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getResponses()).singleElement().satisfies(u -> {
            assertThat(u.getId()).isEqualTo(5L);
            assertThat(u.getEmail()).isEqualTo("user@example.com");
            assertThat(u.getRoles()).containsExactly(Role.TEACHER);
        });
    }

    @Test
    void emptyRepositoryReturnsZeroUsers() {
        when(repository.findAll()).thenReturn(List.of());
        UserResponseList result = service.findAll();
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getResponses()).isEmpty();
    }

    @Test
    void saveEncodesPasswordAndReturnsPersistedUser() {
        when(encoder.encode("secret")).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0); u.setId(9L); return u;
        });
        UserResponse result = service.save(request());
        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRoles()).containsExactly(Role.ADMIN);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void saveDuplicateThrowsAndDoesNotWrite() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.save(request())).isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("user@example.com");
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void updateChangesRolesAndPreservesPassword() {
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        assertThat(service.updateUser(request())).isNull();
        assertThat(existing.getRoles()).containsExactly(Role.ADMIN);
        assertThat(existing.getPassword()).isEqualTo("old-hash");
        assertThat(existing.getEmail()).isEqualTo("user@example.com");
        verifyNoInteractions(encoder);
    }

    @Test
    void updateMissingUserThrows() {
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUser(request())).isInstanceOf(EntityNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletePassesIdToRepository() {
        service.deleteById(5L);
        verify(repository).deleteById(5L);
    }
}
