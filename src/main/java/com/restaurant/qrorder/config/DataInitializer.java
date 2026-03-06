package com.restaurant.qrorder.config;

import com.restaurant.qrorder.domain.common.UserRole;
import com.restaurant.qrorder.domain.entity.Role;
import com.restaurant.qrorder.domain.entity.User;
import com.restaurant.qrorder.repository.RoleRepository;
import com.restaurant.qrorder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initTestUser() {
        return args -> {

            // Ensure STAFF role exists
            createRoleIfNotExists(UserRole.ADMIN);
            createRoleIfNotExists(UserRole.STAFF);
            createRoleIfNotExists(UserRole.CUSTOMER);
            createRoleIfNotExists(UserRole.CHEF);


            createUserIfNotExists(
                    "admin@test.com",
                    "Admin Test User",
                    "0900000001",
                    UserRole.ADMIN
            );

            createUserIfNotExists(
                    "staff@test.com",
                    "Staff Test User",
                    "0900000002",
                    UserRole.STAFF
            );

            createUserIfNotExists(
                    "customer@test.com",
                    "Customer Test User",
                    "0900000003",
                    UserRole.CUSTOMER
            );

            createUserIfNotExists(
                    "chef@test.com",
                    "Chef Test User",
                    "09000000032",
                    UserRole.CHEF
            );
        };
    }

    private void createRoleIfNotExists(UserRole roleName) {
        roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName)
                                .build()
                ));
    }

    private void createUserIfNotExists(
            String email,
            String fullName,
            String phone,
            UserRole roleName
    ) {
        if (userRepository.existsByEmail(email)) return;

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .phone(phone)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }
}



