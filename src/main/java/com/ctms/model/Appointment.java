package com.ctms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType appointmentType; // IMPORT_PICKUP, EXPORT_DROPOFF

    @Column(nullable = false, unique = true, length = 50)
    private String bookingNumber; // Mã Booking hoặc Master B/L

    @Column(nullable = false, length = 20)
    private String containerNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    private Container container;

    @Column(nullable = false, length = 20)
    private String licensePlate; // Biển số xe tải

    private String driverPhone;

    @Column(nullable = false)
    private LocalDateTime appointmentTime;

    private String sealNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    private String notes;

    public enum AppointmentType {
        IMPORT_PICKUP("Lấy hàng Nhập (Import Pickup)"),
        EXPORT_DROPOFF("Hạ hàng Xuất (Export Drop-off)");

        private final String displayName;

        AppointmentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AppointmentStatus {
        PENDING("Chờ duyệt"),
        CONFIRMED("Đã xác nhận"),
        CHECKED_IN("Đang tại cảng"),
        COMPLETED("Đã hoàn tất"),
        CANCELLED("Đã hủy");

        private final String displayName;

        AppointmentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
