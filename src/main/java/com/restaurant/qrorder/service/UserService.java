package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateUserRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateUserRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateUserRoleRequest;
import com.restaurant.qrorder.domain.dto.response.UserResponse;
import com.restaurant.qrorder.domain.entity.Role;
import com.restaurant.qrorder.domain.entity.User;
import com.restaurant.qrorder.repository.RoleRepository;
import com.restaurant.qrorder.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {  // ← no longer implements the interface (it IS the service)

    private final UserRepository userRepository;      // ← added "final"
    private final RoleRepository roleRepository;      // ← added "final"
    private final PasswordEncoder passwordEncoder;    // ← added "final"

    @Transactional                                    // ← removed @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }
        Role role = findRoleById(request.getRoleId());
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .build();
        return toResponse(userRepository.save(user));
    }

    public UserResponse getUserById(Long id) {
        return toResponse(findUserById(id));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllActiveUsers() {
        return userRepository.findAllByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllInactiveUsers() {
        return userRepository.findAllByActiveFalse().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Phone number already in use: " + request.getPhone());
            }
            user.setPhone(request.getPhone());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = findUserById(id);
        Role role = findRoleById(request.getRoleId());
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private Role findRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().getName().name())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}