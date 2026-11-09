package com.example.student.controller;

import com.example.student.dto.*;
import com.example.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student")
@Slf4j
public class StudentController {
    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public StudentResponseList findAll() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public StudentFullResponse findStudentById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @GetMapping("info/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public StudentFullResponse getFullInfoById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse createStudent(@RequestBody StudentRequest studentRequest) {
        return studentService.save(studentRequest);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse updateStudent(@PathVariable Long id, @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStudentById(@PathVariable Long id) {
        studentService.deleteById(id);
    }
}
