package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vessels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 30)
    private String imoNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_line_id", nullable = false)
    private ShippingLine shippingLine;

    private Integer capacityTeu;
    private Double lengthMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VesselStatus status = VesselStatus.SCHEDULED;

    private LocalDateTime eta; // Dự kiến cập bến
    private LocalDateTime etd; // Dự kiến rời bến

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "berth_id")
    private Berth assignedBerth;

    public enum VesselStatus {
        SCHEDULED("Đã lên lịch"),
        BERTHED("Đang cập bến"),
        WORKING("Đang làm hàng"),
        DEPARTED("Đã rời bến");

        private final String displayName;

        VesselStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
