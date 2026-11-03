package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "berths")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Berth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String berthCode; // B-01, B-02

    @Column(nullable = false)
    private String name; // Cầu bến số 1, Cầu bến số 2

    private Double lengthMeters;
    private Double maxDraftMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BerthStatus status = BerthStatus.AVAILABLE;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_vessel_id")
    private Vessel currentVessel;

    public enum BerthStatus {
        AVAILABLE("Cầu bến trống"),
        OCCUPIED("Đang có tàu neo đậu"),
        MAINTENANCE("Bảo trì");

        private final String displayName;

        BerthStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
