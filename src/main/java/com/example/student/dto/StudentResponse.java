package com.example.student.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class StudentResponse {
    private long id;
    private String name;
    private String surname;
}
