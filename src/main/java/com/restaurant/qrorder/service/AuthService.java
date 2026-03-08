package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.UserRole;
import com.restaurant.qrorder.domain.dto.request.LoginRequest;
import com.restaurant.qrorder.domain.dto.request.RegisterRequest;
import com.restaurant.qrorder.domain.dto.response.AuthResponse;
import com.restaurant.qrorder.domain.entity.RestaurantTable;
import com.restaurant.qrorder.domain.entity.Role;
import com.restaurant.qrorder.domain.entity.User;
import com.restaurant.qrorder.exception.custom.AuthenticationException;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.UserMapper;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import com.restaurant.qrorder.repository.RoleRepository;
import com.restaurant.qrorder.repository.UserRepository;
import com.restaurant.qrorder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;
    AuthenticationManager authenticationManager;
    UserDetailsService userDetailsService;
    UserMapper userMapper;
    RestaurantTableRepository tableRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Get CUSTOMER role
        Role customerRole = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER not found"));

        // Create user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(customerRole)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        log.info("User logged in successfully: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(user))
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.validateToken(refreshToken, userDetails)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        String newToken = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(user))
                .build();
    }

    public AuthResponse createGuestSession(String qrCode) {

        RestaurantTable table = tableRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        String guestEmail = "guest_" + table.getTableNumber() + "_" + System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CUSTOMER");
        claims.put("tableId", table.getId());
        claims.put("tableNumber", table.getTableNumber());
        claims.put("guest", true);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                guestEmail,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        String token = jwtUtil.generateToken(claims, userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }
}
