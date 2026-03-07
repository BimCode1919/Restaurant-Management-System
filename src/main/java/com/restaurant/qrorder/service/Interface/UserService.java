package com.restaurant.qrorder.service.Interface;

import com.restaurant.qrorder.domain.dto.request.CreateUserRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateUserRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateUserRoleRequest;
import com.restaurant.qrorder.domain.dto.response.UserResponse;

import java.util.List;
public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    List<UserResponse> getAllActiveUsers();
    List<UserResponse> getAllInactiveUsers();
    UserResponse updateUser(Long id, UpdateUserRequest request);
    UserResponse updateUserRole(Long id, UpdateUserRoleRequest request);
    void deleteUser(Long id);
}
