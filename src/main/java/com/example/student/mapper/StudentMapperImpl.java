package com.example.student.mapper;

import com.example.student.dto.*;
import com.example.student.model.Student;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentMapperImpl implements StudentMapper {
    @Override
    public Student requestToStudent(StudentRequest request) {
        Student student = new Student();
        student.setName(request.getName());
        student.setSurname(request.getSurname());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setEmail(request.getEmail());
        return student;
    }

    @Override
    public StudentFullResponse studentToFullResponse(Student student) {
        return new StudentFullResponse(
                student.getId(),
                student.getName(),
                student.getSurname(),
                student.getPhoneNumber(),
                student.getEmail()
        );
    }

    @Override
    public StudentResponse studentToResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getSurname()
        );
    }

    @Override
    public StudentResponseForList studentToStudentResponseForList(Student student) {
        return new StudentResponseForList(
                student.getId(),
                student.getName(),
                student.getSurname()
        );
    }

    @Override
    public List<StudentResponseForList> studentListToListOfStudentResponse(List<Student> students) {
        return students.stream()
                .map(this::studentToStudentResponseForList)
                .toList();
    }

    @Override
    public StudentResponseList studentListToStudentResponseList(List<Student> students) {
        return new StudentResponseList(
                studentListToListOfStudentResponse(students), students.size()
        );
    }
}
