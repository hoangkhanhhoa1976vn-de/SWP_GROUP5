package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String email;
    private String phone;

    @Column(name = "license_plate", length = 50)
    private String licensePlate; // Biển số xe tải (dành cho tài xế)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_line_id")
    private ShippingLine shippingLine; // Dành cho tài khoản hãng tàu

    public User() {
    }

    public User(Long id, String username, String password, String fullName, Role role, String email, String phone, String licensePlate, ShippingLine shippingLine) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.licensePlate = licensePlate;
        this.shippingLine = shippingLine;
    }
}
