package com.example.student.service;

import com.example.student.dto.*;

public interface StudentService {
    StudentResponse save(StudentRequest request);
    StudentFullResponse findById(Long id);
    StudentResponseList findAll();
    StudentResponse update(Long id, StudentUpdateRequest request);
    void deleteById(Long id);
}
