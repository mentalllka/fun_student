package com.example.student.service;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.model.User;

public interface UserService {
    boolean existsByEmail(String email);
    boolean register(UserRequest userRequest);
    User findByEmail(String email);
    UserResponseList findAll();
    UserResponse save(UserRequest userRequest);
    UserResponse updateUser(UserRequest userRequest);
    void deleteById(Long id);
}
