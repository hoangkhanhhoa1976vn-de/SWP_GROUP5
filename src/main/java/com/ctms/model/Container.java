package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "containers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String containerNumber; // VD: MSKU8291024

    @Column(length = 10)
    private String isoType; // 20GP, 40HC, 45HC

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContainerType containerType; // DRY, REEFER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CargoStatus cargoStatus; // FULL, EMPTY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Condition condition; // GOOD, DAMAGED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrackingStatus currentStatus = TrackingStatus.EXPECTED;

    private String sealNumber; // Số niêm chì
    private Double grossWeightKg; // Tổng trọng lượng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_line_id", nullable = false)
    private ShippingLine shippingLine;

    @OneToOne(mappedBy = "container", fetch = FetchType.LAZY)
    private YardSlot currentSlot;

    private LocalDateTime gateInTime;
    private LocalDateTime gateOutTime;

    public enum ContainerType {
        DRY("Container Khô (Dry)"),
        REEFER("Container Lạnh (Reefer)");

        private final String displayName;

        ContainerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum CargoStatus {
        FULL("Có hàng (Full)"),
        EMPTY("Rỗng (Empty)");

        private final String displayName;

        CargoStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Condition {
        GOOD("Nguyên vẹn (Good)"),
        DAMAGED("Hỏng hóc (Damaged)");

        private final String displayName;

        Condition(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TrackingStatus {
        EXPECTED("Chờ đến (Expected)"),
        GATE_IN("Đã qua cổng vào (Gate In)"),
        IN_YARD("Đã hạ bãi (In Yard)"),
        GATE_OUT("Đã xuất bãi (Gate Out)");

        private final String displayName;

        TrackingStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
