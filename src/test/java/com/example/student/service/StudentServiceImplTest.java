package com.example.student.service;

import com.example.student.dto.*;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.StudentMapperImpl;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {
    @Mock private StudentRepository repository;
    private StudentServiceImpl service;
    private Student existing;

    @BeforeEach
    void setUp() {
        service = new StudentServiceImpl(repository, new StudentMapperImpl());
        existing = new Student();
        existing.setId(7L); existing.setName("Anna"); existing.setSurname("Petrova");
        existing.setPhoneNumber("111"); existing.setEmail("old@example.com");
    }

    @Test
    void saveMapsAllFieldsAndReturnsAssignedId() {
        when(repository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0); s.setId(8L); return s;
        });
        StudentResponse response = service.save(new StudentRequest("Ivan", "Ivanov", "123", "ivan@example.com"));
        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getName()).isEqualTo("Ivan");
        assertThat(response.getSurname()).isEqualTo("Ivanov");
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("123");
        assertThat(captor.getValue().getEmail()).isEqualTo("ivan@example.com");
    }

    @Test
    void findByIdReturnsFullContacts() {
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        StudentFullResponse response = service.findById(7L);
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("Anna");
        assertThat(response.getSurname()).isEqualTo("Petrova");
        assertThat(response.getPhoneNumber()).isEqualTo("111");
        assertThat(response.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    void missingIdThrowsDomainException() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(7L)).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Student with ID 7 not found!");
    }

    @Test
    void findAllMapsNamesAndCount() {
        when(repository.findAll()).thenReturn(List.of(existing));
        StudentResponseList response = service.findAll();
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getResponse()).singleElement().satisfies(s -> {
            assertThat(s.getId()).isEqualTo(7L);
            assertThat(s.getName()).isEqualTo("Anna");
            assertThat(s.getSurname()).isEqualTo("Petrova");
        });
    }

    @Test
    void emptyRepositoryReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll().getResponse()).isEmpty();
        assertThat(service.findAll().getTotalCount()).isZero();
    }

    private StudentResponse update(String phone, String email) {
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        return service.update(7L, new StudentUpdateRequest(phone, email));
    }

    @Test
    void updateBothContactsKeepsNameAndSurname() {
        assertThat(update("222", "new@example.com").getId()).isEqualTo(7L);
        assertThat(existing.getPhoneNumber()).isEqualTo("222");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getName()).isEqualTo("Anna");
        assertThat(existing.getSurname()).isEqualTo("Petrova");
    }

    @Test
    void updateOnlyPhoneKeepsEmail() {
        update("222", null);
        assertThat(existing.getPhoneNumber()).isEqualTo("222");
        assertThat(existing.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    void updateOnlyEmailKeepsPhone() {
        update(null, "new@example.com");
        assertThat(existing.getPhoneNumber()).isEqualTo("111");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void nullContactsLeaveExistingValues() {
        update(null, null);
        assertThat(existing.getPhoneNumber()).isEqualTo("111");
        assertThat(existing.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    void emptyStringsClearContacts() {
        update("", "");
        assertThat(existing.getPhoneNumber()).isEmpty();
        assertThat(existing.getEmail()).isEmpty();
    }

    @Test
    void missingStudentCannotBeUpdated() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(7L, new StudentUpdateRequest("222", "new@example.com")))
                .isInstanceOf(EntityNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteDelegatesExactIdToRepository() {
        service.deleteById(7L);
        verify(repository).deleteById(7L);
    }
}
