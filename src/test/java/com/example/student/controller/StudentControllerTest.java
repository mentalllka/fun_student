package com.example.student.controller;

import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentControllerTest extends ControllerTestSupport {
    @Autowired private StudentRepository repository;

    private Student student() {
        Student s = new Student();
        s.setName("Анна"); s.setSurname("Петрова");
        s.setEmail("anna@example.com"); s.setPhoneNumber("+79001234567");
        return repository.saveAndFlush(s);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminGetsList() throws Exception {
        Student s = student();
        mvc.perform(get("/api/v1/student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value((int) repository.count()))
                .andExpect(jsonPath("$.response[?(@.id == " + s.getId() + ")].name").value(org.hamcrest.Matchers.contains("Анна")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/student/", "/api/v1/student/info/"})
    @WithMockUser(roles = "TEACHER")
    void teacherGetsFullCardByBothRoutes(String prefix) throws Exception {
        Student s = student();
        mvc.perform(get(prefix + s.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(s.getId()))
                .andExpect(jsonPath("$.name").value("Анна"))
                .andExpect(jsonPath("$.surname").value("Петрова"))
                .andExpect(jsonPath("$.phoneNumber").value("+79001234567"))
                .andExpect(jsonPath("$.email").value("anna@example.com"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherGetsList() throws Exception {
        mvc.perform(get("/api/v1/student")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPersistsAllFieldsIncludingSnakeCasePhone() throws Exception {
        long before = repository.count();
        mvc.perform(post("/api/v1/student/save").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Олег","surname":"Иванов","phone_number":"12345","email":"oleg@example.com"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Олег"))
                .andExpect(jsonPath("$.surname").value("Иванов"));
        assertThat(repository.count()).isEqualTo(before + 1);
        Student saved = repository.findAll().stream().filter(s -> "oleg@example.com".equals(s.getEmail())).findFirst().orElseThrow();
        assertThat(saved.getPhoneNumber()).isEqualTo("12345");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateChangesOnlyContacts() throws Exception {
        Student s = student();
        mvc.perform(put("/api/v1/student/" + s.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"999\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(s.getId()));
        Student saved = repository.findById(s.getId()).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPhoneNumber()).isEqualTo("999");
        assertThat(saved.getName()).isEqualTo("Анна");
        assertThat(saved.getSurname()).isEqualTo("Петрова");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRemovesStudent() throws Exception {
        Student s = student();
        mvc.perform(delete("/api/v1/student/" + s.getId())).andExpect(status().isOk());
        assertThat(repository.existsById(s.getId())).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingStudentReturnsStructured404() throws Exception {
        mvc.perform(get("/api/v1/student/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Student with ID 999 not found!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMissingStudentReturns404() throws Exception {
        mvc.perform(put("/api/v1/student/999").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/v1/student/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void malformedJsonAndMissingBodyReturn400() throws Exception {
        mvc.perform(post("/api/v1/student/save").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/student/save").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCannotReadStudents() throws Exception {
        mvc.perform(get("/api/v1/student")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotWriteStudents() throws Exception {
        Student s = student();
        long count = repository.count();
        mvc.perform(post("/api/v1/student/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/student/" + s.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/student/" + s.getId())).andExpect(status().isForbidden());
        assertThat(repository.count()).isEqualTo(count);
        assertThat(repository.findById(s.getId()).orElseThrow().getEmail()).isEqualTo("anna@example.com");
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdminAloneCannotReadStudents() throws Exception {
        mvc.perform(get("/api/v1/student")).andExpect(status().isForbidden());
    }
}
