package com.restaurant.qrorder.domain.entity;

import com.restaurant.qrorder.domain.common.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 20)
    private UserRole name;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    @Builder.Default
    private List<User> users = new ArrayList<>();
}
