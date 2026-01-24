package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.UserRole;
import com.restaurant.qrorder.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(UserRole name);

    boolean existsByName(UserRole name);
}
