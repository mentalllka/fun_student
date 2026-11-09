package com.example.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @JsonProperty("email")
    @Email (message = "Некорректный email")
    @NotBlank (message = "Email обязателен")
    private String email;
    @JsonProperty("password")
    @NotBlank (message = "Пароль обязателен")
    private String password;
}
