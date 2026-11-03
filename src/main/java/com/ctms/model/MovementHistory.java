package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movement_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType eventType;

    @Column(length = 100)
    private String fromLocation;

    @Column(length = 100)
    private String toLocation;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String operatorName; // Người thực hiện

    private String truckPlate; // Biển số xe nếu có

    private String notes;

    public enum MovementType {
        GATE_IN("Vào cổng (Gate In)"),
        ASSIGN_SLOT("Hạ bãi lần đầu (Assign Slot)"),
        MOVE_SLOT("Đảo chuyển vị trí bãi (Move Slot)"),
        GATE_OUT("Rời cảng (Gate Out)");

        private final String displayName;

        MovementType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
