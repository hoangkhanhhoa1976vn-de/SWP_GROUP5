package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yard_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YardSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private YardBlock block; // BLOCK_A, BLOCK_B, BLOCK_C

    @Column(nullable = false, length = 20, unique = true)
    private String slotCode; // VD: A-01, B-08, C-16

    private Integer slotIndex; // 1 -> 16 trong từng block

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    private Container container;

    public boolean isOccupied() {
        return container != null;
    }

    public enum YardBlock {
        BLOCK_A("Khu A (Block A)"),
        BLOCK_B("Khu B (Block B)"),
        BLOCK_C("Khu C (Block C)");

        private final String displayName;

        YardBlock(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
