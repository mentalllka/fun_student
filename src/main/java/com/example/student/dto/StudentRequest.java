package com.example.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StudentRequest {
    private String name;
    private String surname;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String email;
}
