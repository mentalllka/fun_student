package com.example.student.configuration;

import com.example.student.model.Role;
import com.example.student.model.Student;
import com.example.student.model.User;
import com.example.student.repository.StudentRepository;
import com.example.student.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AppInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;

    @PostConstruct
    public void init() {
        createUserIfNotExists("superadmin@example.com", "superAdmin", Set.of(Role.SUPER_ADMIN));
        createUserIfNotExists("admin@example.com", "admin", Set.of(Role.ADMIN));
        createUserIfNotExists("teacher@example.com", "teacher", Set.of(Role.TEACHER));
        createStudentsIfEmpty();
    }

    private void createUserIfNotExists(String email, String rawPassword, Set<Role> roles) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRoles(roles);
            userRepository.save(user);
            System.out.println("Создан пользователь c логином: " + email);
        }
    }
    private void createStudentsIfEmpty() {
        if (studentRepository.count() == 0) {
            Student s1 = new Student();
            s1.setName("Michail");
            s1.setSurname("Ivanov");

            Student s2 = new Student();
            s2.setName("Petr");
            s2.setSurname("Semenov");

            Student s3 = new Student();
            s3.setName("Sergey");
            s3.setSurname("Boroda");

            Student s4 = new Student();
            s4.setName("Ekaterina");
            s4.setSurname("Sidorova");

            Student s5 = new Student();
            s5.setName("Dmitry");
            s5.setSurname("Alexandrov");

            studentRepository.save(s1);
            studentRepository.save(s2);
            studentRepository.save(s3);
            studentRepository.save(s4);
            studentRepository.save(s5);

            System.out.println("Созданы тестовые записи студентов.");
        }
    }
}
