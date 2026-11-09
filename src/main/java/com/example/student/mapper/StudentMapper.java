package com.example.student.mapper;

import com.example.student.dto.*;
import com.example.student.model.Student;

import java.util.List;

public interface StudentMapper {
    Student requestToStudent(StudentRequest request);
    StudentFullResponse studentToFullResponse(Student student);
    StudentResponse studentToResponse(Student student);
    StudentResponseForList studentToStudentResponseForList(Student student);
    List<StudentResponseForList> studentListToListOfStudentResponse(List<Student> students);
    StudentResponseList studentListToStudentResponseList(List<Student> students);
}
