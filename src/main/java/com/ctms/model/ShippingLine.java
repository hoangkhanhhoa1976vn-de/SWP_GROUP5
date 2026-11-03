package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipping_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code; // VD: MAE, MSC, CMA

    @Column(nullable = false)
    private String name; // Maersk Line, MSC, etc.

    private String contactEmail;
    private String contactPhone;
    private String address;

    @OneToMany(mappedBy = "shippingLine", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Vessel> vessels = new ArrayList<>();
}
