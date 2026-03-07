package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.active = true")
    List<User> findByRoleNameAndActive(String roleName);
    List<User> findAllByActiveTrue();
    List<User> findAllByActiveFalse();
    boolean existsByPhone(String phone);
}
