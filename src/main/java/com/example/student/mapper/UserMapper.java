package com.example.student.mapper;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.model.User;

import java.util.List;

public interface UserMapper {
    UserResponse toResponse(User user);
    User toEntity(UserRequest userRequest);
    List<UserResponse> toResponseList(List<User> users);
    UserResponseList toListResponse(List<User> users);

}
