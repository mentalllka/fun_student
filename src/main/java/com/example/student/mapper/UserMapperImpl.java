package com.example.student.mapper;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapperImpl implements UserMapper {
    @Override
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRoles());
    }

    @Override
    public User toEntity(UserRequest userRequest) {
        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword());
        user.setRoles(userRequest.getRoles());
        return user;
    }

    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponseList toListResponse(List<User> users) {
        List<UserResponse> responses = toResponseList(users);
        return new UserResponseList(responses, responses.size());
    }
}
