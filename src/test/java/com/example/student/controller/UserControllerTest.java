package com.example.student.controller;

import com.example.student.model.Role;
import com.example.student.model.User;
import com.example.student.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends ControllerTestSupport {
    @Autowired private UserRepository repository;
    @Autowired private PasswordEncoder encoder;

    private User user() {
        return repository.saveAndFlush(User.builder().email("test-user@example.com")
                .password(encoder.encode("secret")).roles(Set.of(Role.TEACHER)).build());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listIncludesRolesButNeverPasswords() throws Exception {
        user();
        mvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value((int) repository.count()))
                .andExpect(jsonPath("$.responses").isArray())
                .andExpect(jsonPath("$.responses[*].password").doesNotExist())
                .andExpect(jsonPath("$.responses[?(@.email == 'test-user@example.com')].roles[0]").value(org.hamcrest.Matchers.contains("TEACHER")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createHashesPasswordAndPersistsRoles() throws Exception {
        mvc.perform(post("/api/v1/users/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"created@example.com\",\"password\":\"secret\",\"roles\":[\"ADMIN\",\"TEACHER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("created@example.com"))
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.password").doesNotExist());
        User saved = repository.findByEmail("created@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("secret");
        assertThat(encoder.matches("secret", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.TEACHER);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateChangesRolesAndKeepsPassword() throws Exception {
        User u = user();
        String oldHash = u.getPassword();
        // Исходный сервис ищет по email и возвращает null. Проверяем фактические данные в БД.
        mvc.perform(put("/api/v1/users/" + u.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-user@example.com\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk()).andExpect(content().string(""));
        User saved = repository.findByEmail(u.getEmail()).orElseThrow();
        assertThat(saved.getRoles()).containsExactly(Role.ADMIN);
        assertThat(saved.getPassword()).isEqualTo(oldHash);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteRemovesUser() throws Exception {
        User u = user();
        mvc.perform(delete("/api/v1/users/" + u.getId())).andExpect(status().isOk());
        assertThat(repository.existsById(u.getId())).isFalse();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void missingEmailOnUpdateReturns404() throws Exception {
        mvc.perform(put("/api/v1/users/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void unknownRoleReturns400() throws Exception {
        mvc.perform(post("/api/v1/users/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad-role@example.com\",\"password\":\"secret\",\"roles\":[\"UNKNOWN\"]}"))
                .andExpect(status().isBadRequest());
        assertThat(repository.existsByEmail("bad-role@example.com")).isFalse();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void nonNumericDeleteIdReturns400() throws Exception {
        mvc.perform(delete("/api/v1/users/abc")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TEACHER"})
    void ordinaryRolesCannotAccessAnyUserOperation(String role) throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("other").roles(role);
        mvc.perform(get("/api/v1/users").with(auth)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users/save").with(auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/1").with(auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/users/1").with(auth)).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadUsers() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
    }
}
