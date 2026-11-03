package com.ctms.service;

import com.ctms.model.Appointment;
import com.ctms.model.Container;
import com.ctms.model.ShippingLine;
import com.ctms.model.User;
import com.ctms.model.YardSlot;
import com.ctms.repository.AppointmentRepository;
import com.ctms.repository.ContainerRepository;
import com.ctms.repository.ShippingLineRepository;
import com.ctms.repository.YardSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AppointmentService {

    /** Số container theo ISO 6346: 4 chữ cái + 7 chữ số (VD: MSKU1234567). */
    private static final Pattern CONTAINER_NO = Pattern.compile("^[A-Z]{4}\\d{7}$");
    private static final Set<Appointment.AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(Appointment.AppointmentStatus.PENDING,
                       Appointment.AppointmentStatus.CONFIRMED,
                       Appointment.AppointmentStatus.CHECKED_IN);

    private final AppointmentRepository appointmentRepository;
    private final ContainerRepository containerRepository;
    private final YardSlotRepository yardSlotRepository;
    private final ShippingLineRepository shippingLineRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              ContainerRepository containerRepository,
                              YardSlotRepository yardSlotRepository,
                              ShippingLineRepository shippingLineRepository) {
        this.appointmentRepository = appointmentRepository;
        this.containerRepository = containerRepository;
        this.yardSlotRepository = yardSlotRepository;
        this.shippingLineRepository = shippingLineRepository;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentTimeDesc();
    }

    public List<Appointment> getAppointmentsForDriver(Long driverId) {
        return appointmentRepository.findByDriverIdOrderByAppointmentTimeDesc(driverId);
    }

    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Optional<Appointment> findByBookingNumber(String bookingNumber) {
        return appointmentRepository.findByBookingNumber(bookingNumber);
    }

    @Transactional
    public Appointment createAppointment(User driver,
                                         Appointment.AppointmentType type,
                                         String bookingNumber,
                                         String containerNumber,
                                         String licensePlate,
                                         String phone,
                                         LocalDateTime appointmentTime,
                                         String sealNumber,
                                         String notes) {
        // ---- 1. Kiểm tra dữ liệu đầu vào ----
        if (driver == null) {
            throw new IllegalArgumentException("Không xác định được tài khoản đặt lịch!");
        }
        if (type == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại lịch hẹn!");
        }
        String cleanBooking = bookingNumber == null ? "" : bookingNumber.trim().toUpperCase();
        if (cleanBooking.isEmpty()) {
            throw new IllegalArgumentException("Mã booking không được để trống!");
        }
        if (cleanBooking.length() > 50) {
            throw new IllegalArgumentException("Mã booking quá dài (tối đa 50 ký tự)!");
        }
        String cleanContainerNo = containerNumber == null ? "" : containerNumber.trim().toUpperCase();
        if (!CONTAINER_NO.matcher(cleanContainerNo).matches()) {
            throw new IllegalArgumentException("Số container không hợp lệ (4 chữ cái + 7 chữ số, ví dụ MSKU1234567)!");
        }
        String cleanPlate = licensePlate == null ? "" : licensePlate.trim().toUpperCase();
        if (cleanPlate.isEmpty()) {
            throw new IllegalArgumentException("Biển số xe không được để trống!");
        }
        if (appointmentTime == null || appointmentTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Thời gian hẹn phải ở hiện tại hoặc tương lai!");
        }
        if (appointmentRepository.existsByBookingNumber(cleanBooking)) {
            throw new IllegalArgumentException("Mã booking " + cleanBooking + " đã tồn tại!");
        }
        if (appointmentRepository.existsByContainerNumberAndStatusIn(cleanContainerNo, ACTIVE_STATUSES)) {
            throw new IllegalStateException("Container " + cleanContainerNo + " đã có lịch hẹn đang hoạt động!");
        }

        String cleanSeal = (sealNumber != null && !sealNumber.trim().isEmpty()) ? sealNumber.trim().toUpperCase() : null;
        Container linkedContainer;
        String assignedSlotCode = "";

        if (type == Appointment.AppointmentType.EXPORT_DROPOFF) {
            // Hạ cont Xuất: tạo (hoặc dùng lại) container ở trạng thái EXPECTED.
            // KHÔNG giữ chỗ ô bãi tại thời điểm đặt lịch; operator sẽ gán ô khi container qua cổng.
            Optional<Container> existing = containerRepository.findByContainerNumber(cleanContainerNo);
            if (existing.isPresent()) {
                linkedContainer = existing.get();
                Container.TrackingStatus st = linkedContainer.getCurrentStatus();
                if (st == Container.TrackingStatus.GATE_IN || st == Container.TrackingStatus.IN_YARD) {
                    throw new IllegalStateException("Container " + cleanContainerNo + " đang ở trong cảng, không thể đặt hạ bãi!");
                }
                linkedContainer.setCurrentStatus(Container.TrackingStatus.EXPECTED);
                linkedContainer.setGateOutTime(null);
            } else {
                linkedContainer = Container.builder()
                        .containerNumber(cleanContainerNo)
                        .isoType("40HC")
                        .containerType(Container.ContainerType.DRY)
                        .cargoStatus(Container.CargoStatus.FULL)
                        .condition(Container.Condition.GOOD)
                        .currentStatus(Container.TrackingStatus.EXPECTED)
                        .shippingLine(resolveShippingLine(cleanContainerNo))
                        .build();
            }
            if (cleanSeal != null) {
                linkedContainer.setSealNumber(cleanSeal);
            }
            linkedContainer = containerRepository.save(linkedContainer);
        } else {
            // Lấy cont Nhập: container phải đã có trong hệ thống và đã hạ bãi.
            linkedContainer = containerRepository.findByContainerNumber(cleanContainerNo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy container " + cleanContainerNo + " trong hệ thống. Vui lòng kiểm tra lại số container!"));
            if (linkedContainer.getCurrentStatus() != Container.TrackingStatus.IN_YARD) {
                throw new IllegalStateException("Container " + cleanContainerNo
                        + " chưa sẵn sàng để lấy (trạng thái: " + (linkedContainer.getCurrentStatus() == null ? "chưa xác định" : linkedContainer.getCurrentStatus().getDisplayName()) + ")!");
            }
            Optional<YardSlot> slotOpt = yardSlotRepository.findByContainer(linkedContainer);
            if (slotOpt.isPresent()) {
                assignedSlotCode = slotOpt.get().getSlotCode();
                linkedContainer.setCurrentSlot(slotOpt.get());
            }
        }

        String fullNotes = (notes != null && !notes.trim().isEmpty()) ? notes.trim() : "";
        if (!assignedSlotCode.isEmpty()) {
            fullNotes = (fullNotes.isEmpty() ? "" : fullNotes + " | ") + "Ô bãi: " + assignedSlotCode;
        }

        Appointment appointment = Appointment.builder()
                .driver(driver)
                .appointmentType(type)
                .bookingNumber(cleanBooking)
                .containerNumber(cleanContainerNo)
                .container(linkedContainer)
                .licensePlate(cleanPlate)
                .driverPhone(phone)
                .appointmentTime(appointmentTime)
                .sealNumber(cleanSeal)
                .notes(fullNotes)
                .status(Appointment.AppointmentStatus.CONFIRMED)
                .build();

        return appointmentRepository.save(appointment);
    }

    /** Ưu tiên hãng tàu có mã trùng tiền tố số container; nếu không có thì lấy hãng đầu tiên. */
    private ShippingLine resolveShippingLine(String containerNo) {
        List<ShippingLine> lines = shippingLineRepository.findAll();
        return lines.stream()
                .filter(l -> l.getCode() != null && containerNo.startsWith(l.getCode().toUpperCase()))
                .findFirst()
                .orElseGet(() -> lines.stream().findFirst().orElse(null));
    }
}
