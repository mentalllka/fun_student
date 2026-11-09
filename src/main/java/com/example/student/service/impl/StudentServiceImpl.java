package com.example.student.service.impl;

import com.example.student.dto.*;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.StudentMapper;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Transactional
    @Override
    public StudentResponse save(StudentRequest studentRequest) {
        return studentMapper.studentToResponse(
                studentRepository.save(studentMapper.requestToStudent(studentRequest)));
    }

    @Transactional(readOnly = true)
    @Override
    public StudentFullResponse findById(Long id) {
        return studentMapper.studentToFullResponse(getStudentById(id));
    }

    @Transactional(readOnly = true)
    @Override
    public StudentResponseList findAll() {
        return studentMapper.studentListToStudentResponseList(studentRepository.findAll());
    }

    @Transactional
    @Override
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student existedStudent = getStudentById(id);
        if(request.getPhoneNumber() != null) existedStudent.setPhoneNumber(request.getPhoneNumber());
        if(request.getEmail() != null) existedStudent.setEmail(request.getEmail());
        return studentMapper.studentToResponse(studentRepository.save(existedStudent));
    }

    @Override
    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }

    private Student getStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow(()->new EntityNotFoundException(
                MessageFormat.format("Student with ID {0} not found!", id)
        ));
    }
}
