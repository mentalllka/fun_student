package com.example.student.configuration;

import com.example.student.model.*;
import com.example.student.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AppInitializerTest {
    private final UserRepository users = mock(UserRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AppInitializer initializer = new AppInitializer(users, encoder, students);

    @Test
    void emptyDatabaseReceivesThreeUsersAndFiveStudents() {
        when(encoder.encode(anyString())).thenAnswer(i -> "encoded-" + i.getArgument(0));
        initializer.init();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users, times(3)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues()).extracting(User::getEmail)
                .containsExactly("superadmin@example.com", "admin@example.com", "teacher@example.com");
        assertThat(userCaptor.getAllValues()).extracting(User::getPassword)
                .containsExactly("encoded-superAdmin", "encoded-admin", "encoded-teacher");
        assertThat(userCaptor.getAllValues().get(0).getRoles()).containsExactly(Role.SUPER_ADMIN);
        assertThat(userCaptor.getAllValues().get(1).getRoles()).containsExactly(Role.ADMIN);
        assertThat(userCaptor.getAllValues().get(2).getRoles()).containsExactly(Role.TEACHER);
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(students, times(5)).save(studentCaptor.capture());
        assertThat(studentCaptor.getAllValues()).extracting(Student::getName)
                .containsExactly("Michail", "Petr", "Sergey", "Ekaterina", "Dmitry");
        assertThat(studentCaptor.getAllValues()).extracting(Student::getSurname)
                .containsExactly("Ivanov", "Semenov", "Boroda", "Sidorova", "Alexandrov");
    }

    @Test
    void existingDataIsNotDuplicatedOrOverwritten() {
        when(users.existsByEmail(anyString())).thenReturn(true);
        when(students.count()).thenReturn(5L);
        initializer.init();
        verify(users, never()).save(any());
        verify(students, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void onlyMissingAccountsAreCreated() {
        when(users.existsByEmail("superadmin@example.com")).thenReturn(true);
        when(users.existsByEmail("admin@example.com")).thenReturn(false);
        when(users.existsByEmail("teacher@example.com")).thenReturn(true);
        when(students.count()).thenReturn(1L);
        when(encoder.encode("admin")).thenReturn("encoded-admin");
        initializer.init();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
        verify(students, never()).save(any());
    }
}
