package com.example.student.controller;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.mapper.UserMapper;
import com.example.student.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {
    private final UserService userService;

    @GetMapping
    public UserResponseList getAllUsers() {
        return userService.findAll();
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@RequestBody UserRequest userRequest) {
        return userService.updateUser(userRequest);
    }

    @PostMapping("/save")
    public UserResponse save(@RequestBody UserRequest userRequest) {
        return userService.save(userRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        userService.deleteById(id);
    }
}
