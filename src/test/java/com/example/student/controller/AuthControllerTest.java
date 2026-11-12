package com.example.student.controller;

import com.example.student.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends ControllerTestSupport {
    @Autowired private JwtService jwtService;

    @ParameterizedTest(name = "Вход {0}: роль {2}")
    @CsvSource({
            "admin@example.com, admin, ADMIN",
            "teacher@example.com, teacher, TEACHER",
            "superadmin@example.com, superAdmin, SUPER_ADMIN"
    })
    void loginReturnsUsableToken(String email, String password, String role) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_" + role))
                .andReturn().getResponse().getContentAsString();
        String token = JsonMapper.builder().build().readTree(response).get("token").asText();
        assertThat(jwtService.extractUsername(token)).isEqualTo(email);
        assertThat(jwtService.extractRoles(token)).extracting(Enum::name).containsExactly(role);
        mvc.perform(get(role.equals("SUPER_ADMIN") ? "/api/v1/users" : "/api/v1/student")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "Невалидный запрос входа: {0}")
    @ValueSource(strings = {
            "{}", "{\"email\":null,\"password\":null}",
            "{\"email\":\"\",\"password\":\"admin\"}",
            "{\"email\":\"   \",\"password\":\"admin\"}",
            "{\"email\":\"not-an-email\",\"password\":\"admin\"}",
            "{\"email\":\"admin@example.com\"}",
            "{\"email\":\"admin@example.com\",\"password\":\"\"}",
            "{\"email\":\"admin@example.com\",\"password\":\"   \"}"
    })
    void invalidLoginFieldsReturn400(String json) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"admin@example.com, wrong", "missing@example.com, admin"})
    void wrongCredentialsAreRejected(String email, String password) throws Exception {
        // В исходной конфигурации Security возвращает 403 при неверных данных входа.
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonJsonContentReturns415() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("admin"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/style.css", "/app.js"})
    void frontendIsAvailableWithoutLogin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }
}
