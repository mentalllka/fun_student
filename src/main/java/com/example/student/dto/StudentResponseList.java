package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@AllArgsConstructor
@Getter
@Setter
public class StudentResponseList {
    List<StudentResponseForList> response;
    int totalCount;
}
