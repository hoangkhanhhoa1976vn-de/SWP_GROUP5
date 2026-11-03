package com.ctms.service;

import com.ctms.model.Appointment;
import com.ctms.model.Container;
import com.ctms.model.MovementHistory;
import com.ctms.model.ShippingLine;
import com.ctms.model.YardSlot;
import com.ctms.repository.AppointmentRepository;
import com.ctms.repository.ContainerRepository;
import com.ctms.repository.MovementHistoryRepository;
import com.ctms.repository.ShippingLineRepository;
import com.ctms.repository.YardSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
public class GateService {

    private final ContainerRepository containerRepository;
    private final AppointmentRepository appointmentRepository;
    private final MovementHistoryRepository movementHistoryRepository;
    private final YardSlotRepository yardSlotRepository;
    private final ShippingLineRepository shippingLineRepository;

    public GateService(ContainerRepository containerRepository,
                       AppointmentRepository appointmentRepository,
                       MovementHistoryRepository movementHistoryRepository,
                       YardSlotRepository yardSlotRepository,
                       ShippingLineRepository shippingLineRepository) {
        this.containerRepository = containerRepository;
        this.appointmentRepository = appointmentRepository;
        this.movementHistoryRepository = movementHistoryRepository;
        this.yardSlotRepository = yardSlotRepository;
        this.shippingLineRepository = shippingLineRepository;
    }

    /**
     * Use Case phụ: <<include>> Verify Container (Seal, Condition, Booking)
     */
    public VerificationResult verifyContainer(String containerNumber, String sealNumber, String bookingNumber) {
        boolean valid = true;
        StringBuilder message = new StringBuilder();

        if (containerNumber == null || containerNumber.trim().isEmpty()) {
            return new VerificationResult(false, "Mã container không được để trống!");
        }

        if (sealNumber == null || sealNumber.trim().isEmpty()) {
            valid = false;
            message.append("Thiếu số chì (Seal) niêm phong! ");
        }

        if (bookingNumber != null && !bookingNumber.trim().isEmpty()) {
            Optional<Appointment> appOpt = appointmentRepository.findByBookingNumber(bookingNumber.trim());
            if (appOpt.isPresent()) {
                message.append("Khớp thông tin Booking: ").append(bookingNumber).append(". ");
            } else {
                message.append("Thông tin Booking nhập thủ công. ");
            }
        }

        return new VerificationResult(valid, message.toString());
    }

    /**
     * Use Case chính: Confirm Gate In
     * <<include>> Verify Container
     * <<include>> Record Container Movement
     */
    @Transactional
    public Container confirmGateIn(String containerNumber,
                                   String sealNumber,
                                   Container.ContainerType containerType,
                                   Container.CargoStatus cargoStatus,
                                   Container.Condition condition,
                                   String bookingNumber,
                                   String licensePlate,
                                   String operatorName) {

        String cleanNo = containerNumber == null ? "" : containerNumber.trim().toUpperCase();
        String cleanSeal = sealNumber == null ? "" : sealNumber.trim().toUpperCase();
        String cleanPlate = licensePlate == null ? "" : licensePlate.trim().toUpperCase();
        String cleanBooking = (bookingNumber == null || bookingNumber.trim().isEmpty())
                ? null : bookingNumber.trim().toUpperCase();

        // 1. Kiểm tra xác minh (<<include>> Verify Container) - nếu không hợp lệ thì TỪ CHỐI
        VerificationResult verification = verifyContainer(cleanNo, cleanSeal, cleanBooking);
        if (!verification.isValid()) {
            throw new IllegalArgumentException(verification.getMessage().trim());
        }
        if (cleanPlate.isEmpty()) {
            throw new IllegalArgumentException("Biển số xe không được để trống!");
        }

        // 2. Kiểm tra lịch hẹn (nếu có nhập booking)
        Appointment appointment = null;
        if (cleanBooking != null) {
            appointment = appointmentRepository.findByBookingNumber(cleanBooking).orElse(null);
            if (appointment != null) {
                if (appointment.getStatus() != Appointment.AppointmentStatus.CONFIRMED) {
                    throw new IllegalStateException("Lịch hẹn " + cleanBooking + " đang ở trạng thái '"
                            + appointment.getStatus().getDisplayName() + "', không thể xác nhận vào cổng!");
                }
                if (!cleanNo.equalsIgnoreCase(appointment.getContainerNumber())) {
                    throw new IllegalArgumentException("Số container " + cleanNo
                            + " không khớp với lịch hẹn (" + appointment.getContainerNumber() + ")!");
                }
            }
        }

        // 3. Tìm hoặc khởi tạo container
        Container container = containerRepository.findByContainerNumber(cleanNo).orElse(null);
        if (container != null && (container.getCurrentStatus() == Container.TrackingStatus.GATE_IN
                || container.getCurrentStatus() == Container.TrackingStatus.IN_YARD)) {
            throw new IllegalStateException("Container " + cleanNo + " đang ở trong cảng, không thể vào cổng lần nữa!");
        }
        if (container == null) {
            ShippingLine sl = null;
            if (appointment != null && appointment.getContainer() != null && appointment.getContainer().getShippingLine() != null) {
                sl = appointment.getContainer().getShippingLine();
            }
            if (sl == null) {
                sl = resolveShippingLine(cleanNo);
            }
            container = Container.builder()
                    .containerNumber(cleanNo)
                    .containerType(containerType != null ? containerType : Container.ContainerType.DRY)
                    .isoType("40HC")
                    .shippingLine(sl)
                    .build();
        } else if (container.getShippingLine() == null) {
            container.setShippingLine(resolveShippingLine(cleanNo));
        }
        container.setSealNumber(cleanSeal);
        container.setCondition(condition != null ? condition : Container.Condition.GOOD);
        container.setCargoStatus(cargoStatus != null ? cargoStatus : Container.CargoStatus.FULL);
        container.setCurrentStatus(Container.TrackingStatus.GATE_IN);
        container.setGateInTime(LocalDateTime.now());
        container.setGateOutTime(null);
        final Container savedContainer = containerRepository.save(container);

        // 4. Cập nhật lịch hẹn nếu có
        if (appointment != null) {
            appointment.setStatus(Appointment.AppointmentStatus.CHECKED_IN);
            appointment.setContainer(savedContainer);
            appointmentRepository.save(appointment);
        }

        // 5. Tự động ghi nhật ký di chuyển (<<include>> Record Container Movement)
        String plateNote = (appointment != null && appointment.getLicensePlate() != null
                && !appointment.getLicensePlate().equalsIgnoreCase(cleanPlate))
                ? " Lưu ý: biển số xe thực tế khác lịch hẹn (" + appointment.getLicensePlate() + ")." : "";
        MovementHistory movement = MovementHistory.builder()
                .container(savedContainer)
                .eventType(MovementHistory.MovementType.GATE_IN)
                .fromLocation("Cổng tiếp nhận vào (Gate-In)")
                .toLocation("Bãi chờ hạ container")
                .truckPlate(cleanPlate)
                .operatorName(operatorName != null ? operatorName : "Gate Operator")
                .timestamp(LocalDateTime.now())
                .notes("Xe " + cleanPlate + " vào cổng thành công. Seal: " + cleanSeal + ". "
                        + verification.getMessage() + plateNote)
                .build();
        movementHistoryRepository.save(movement);

        return savedContainer;
    }

    /**
     * Use Case chính: Confirm Gate Out
     * <<include>> Verify Container
     * <<include>> Record Container Movement
     */
    @Transactional
    public Container confirmGateOut(String containerNumber,
                                    String sealNumber,
                                    Container.Condition condition,
                                    String licensePlate,
                                    String operatorName) {

        String cleanNo = containerNumber == null ? "" : containerNumber.trim().toUpperCase();
        String cleanSeal = sealNumber == null ? "" : sealNumber.trim().toUpperCase();
        String cleanPlate = licensePlate == null ? "" : licensePlate.trim().toUpperCase();

        Container container = containerRepository.findByContainerNumber(cleanNo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy container: " + cleanNo));

        // 1. Container phải đang nằm trong bãi
        if (container.getCurrentStatus() != Container.TrackingStatus.IN_YARD) {
            throw new IllegalStateException("Container " + cleanNo + " không ở trạng thái 'Đã hạ bãi' nên không thể xuất cổng!");
        }
        if (cleanPlate.isEmpty()) {
            throw new IllegalArgumentException("Biển số xe không được để trống!");
        }

        // 2. Đối chiếu số chì (seal)
        if (cleanSeal.isEmpty()) {
            throw new IllegalArgumentException("Thiếu số chì (Seal) niêm phong!");
        }
        if (container.getSealNumber() != null && !container.getSealNumber().isBlank()
                && !container.getSealNumber().trim().equalsIgnoreCase(cleanSeal)) {
            throw new IllegalArgumentException("Số chì không khớp với hồ sơ container (" + container.getSealNumber() + ")!");
        }

        // 3. Giải phóng ô bãi
        Optional<YardSlot> slotOpt = yardSlotRepository.findByContainer(container);
        String fromLoc = "Khu vực bãi";
        if (slotOpt.isPresent()) {
            YardSlot slot = slotOpt.get();
            fromLoc = "Ô bãi " + slot.getSlotCode();
            slot.setContainer(null);
            yardSlotRepository.save(slot);
        }

        // 4. Cập nhật trạng thái container
        container.setCurrentStatus(Container.TrackingStatus.GATE_OUT);
        container.setGateOutTime(LocalDateTime.now());
        container.setCondition(condition != null ? condition : container.getCondition());
        if (container.getSealNumber() == null || container.getSealNumber().isBlank()) {
            container.setSealNumber(cleanSeal);
        }
        containerRepository.save(container);

        // 5. Hoàn tất lịch hẹn đang hoạt động của container này (nếu có)
        appointmentRepository.findFirstByContainerNumberAndStatusInOrderByAppointmentTimeDesc(
                container.getContainerNumber(),
                EnumSet.of(Appointment.AppointmentStatus.CONFIRMED, Appointment.AppointmentStatus.CHECKED_IN))
                .ifPresent(app -> {
                    app.setStatus(Appointment.AppointmentStatus.COMPLETED);
                    appointmentRepository.save(app);
                });

        // 6. Ghi nhật ký di chuyển (<<include>> Record Container Movement)
        MovementHistory movement = MovementHistory.builder()
                .container(container)
                .eventType(MovementHistory.MovementType.GATE_OUT)
                .fromLocation(fromLoc)
                .toLocation("Rời cảng (Gate-Out)")
                .truckPlate(cleanPlate)
                .operatorName(operatorName != null ? operatorName : "Gate Operator")
                .timestamp(LocalDateTime.now())
                .notes("Xe " + cleanPlate + " nhận cont xuất cổng. Kiểm tra seal: " + cleanSeal)
                .build();
        movementHistoryRepository.save(movement);

        return container;
    }

    private ShippingLine resolveShippingLine(String containerNo) {
        List<ShippingLine> lines = shippingLineRepository.findAll();
        return lines.stream()
                .filter(l -> l.getCode() != null && containerNo.startsWith(l.getCode().toUpperCase()))
                .findFirst()
                .orElseGet(() -> lines.stream().findFirst().orElse(null));
    }

    @lombok.Value
    public static class VerificationResult {
        boolean valid;
        String message;
    }
}
