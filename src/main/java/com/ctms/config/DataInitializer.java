package com.ctms.config;

import com.ctms.model.*;
import com.ctms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShippingLineRepository shippingLineRepository;
    private final BerthRepository berthRepository;
    private final VesselRepository vesselRepository;
    private final ContainerRepository containerRepository;
    private final YardSlotRepository yardSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final MovementHistoryRepository movementHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    /** Mật khẩu cho các tài khoản mẫu. Đặt qua biến môi trường CTMS_SEED_PASSWORD. */
    @Value("${ctms.seed.password:}")
    private String seedPassword;

    /** Bật/tắt nạp dữ liệu mẫu (nên tắt ở môi trường thật). */
    @Value("${ctms.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Nạp dữ liệu mẫu đang tắt (ctms.seed.enabled=false).");
            return;
        }
        if (seedPassword == null || seedPassword.isBlank()) {
            log.warn("Bỏ qua nạp dữ liệu mẫu: chưa đặt CTMS_SEED_PASSWORD.");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Dữ liệu ban đầu đã tồn tại.");
            return;
        }

        log.info("Khởi tạo dữ liệu mẫu cho hệ thống CTMS...");

        // 1. Tạo Hãng tàu
        ShippingLine maersk = shippingLineRepository.save(ShippingLine.builder()
                .code("MAE")
                .name("Maersk Line")
                .contactEmail("operations@maersk.com")
                .contactPhone("028-3823-0001")
                .address("Tòa nhà Bitexco, Q.1, TP.HCM")
                .build());

        ShippingLine msc = shippingLineRepository.save(ShippingLine.builder()
                .code("MSC")
                .name("Mediterranean Shipping Company")
                .contactEmail("portops@msc.com")
                .contactPhone("028-3823-0002")
                .address("Tòa nhà Sunwah, Q.1, TP.HCM")
                .build());

        ShippingLine cma = shippingLineRepository.save(ShippingLine.builder()
                .code("CMA")
                .name("CMA CGM Group")
                .contactEmail("contact@cma-cgm.com")
                .contactPhone("028-3823-0003")
                .address("Tòa nhà Saigon Tower, Q.1, TP.HCM")
                .build());

        // 2. Tạo Người dùng (4 Actor chính)
        User manager = userRepository.save(User.builder()
                .username("manager")
                .password(passwordEncoder.encode(seedPassword))
                .fullName("Nguyễn Văn Quản Lý")
                .role(Role.ROLE_MANAGER)
                .email("manager@ctms.port.vn")
                .phone("0901234567")
                .build());

        User operator = userRepository.save(User.builder()
                .username("operator")
                .password(passwordEncoder.encode(seedPassword))
                .fullName("Trần Điều Hành")
                .role(Role.ROLE_OPERATOR)
                .email("operator@ctms.port.vn")
                .phone("0902345678")
                .build());

        User shippingUser = userRepository.save(User.builder()
                .username("shipping_maersk")
                .password(passwordEncoder.encode(seedPassword))
                .fullName("Đại Diện Hãng Tàu Maersk")
                .role(Role.ROLE_SHIPPING_LINE)
                .shippingLine(maersk)
                .email("agent@maersk.com")
                .phone("0903456789")
                .build());

        User driver = userRepository.save(User.builder()
                .username("driver")
                .password(passwordEncoder.encode(seedPassword))
                .fullName("Lê Văn Tài Xế")
                .role(Role.ROLE_TRUCK_DRIVER)
                .email("driver01@transport.vn")
                .phone("0904567890")
                .build());

        // 3. Tạo Cầu bến (Berths)
        Berth berth1 = berthRepository.save(Berth.builder()
                .berthCode("B-01")
                .name("Cầu cảng số 1 (Ngoại thương)")
                .lengthMeters(380.0)
                .maxDraftMeters(14.5)
                .status(Berth.BerthStatus.OCCUPIED)
                .build());

        Berth berth2 = berthRepository.save(Berth.builder()
                .berthCode("B-02")
                .name("Cầu cảng số 2 (Nội địa & trung chuyển)")
                .lengthMeters(420.0)
                .maxDraftMeters(16.0)
                .status(Berth.BerthStatus.AVAILABLE)
                .build());

        // 4. Tạo Tàu (Vessels)
        Vessel vessel1 = vesselRepository.save(Vessel.builder()
                .name("MSC OSCAR")
                .imoNumber("IMO9703291")
                .shippingLine(msc)
                .capacityTeu(19224)
                .lengthMeters(395.4)
                .status(Vessel.VesselStatus.BERTHED)
                .eta(LocalDateTime.now().minusHours(8))
                .etd(LocalDateTime.now().plusHours(16))
                .assignedBerth(berth1)
                .build());

        berth1.setCurrentVessel(vessel1);
        berthRepository.save(berth1);

        Vessel vessel2 = vesselRepository.save(Vessel.builder()
                .name("MAERSK MC-KINNEY MOLLER")
                .imoNumber("IMO9619907")
                .shippingLine(maersk)
                .capacityTeu(18270)
                .lengthMeters(399.0)
                .status(Vessel.VesselStatus.SCHEDULED)
                .eta(LocalDateTime.now().plusDays(1))
                .etd(LocalDateTime.now().plusDays(2))
                .build());

        Vessel vessel3 = vesselRepository.save(Vessel.builder()
                .name("CMA CGM ANTOINE DE SAINT EXUPERY")
                .imoNumber("IMO9776418")
                .shippingLine(cma)
                .capacityTeu(20600)
                .lengthMeters(400.0)
                .status(Vessel.VesselStatus.SCHEDULED)
                .eta(LocalDateTime.now().plusDays(2))
                .etd(LocalDateTime.now().plusDays(3))
                .build());

        // 5. Khởi tạo 48 ô bãi (Yard Slots: Block A: 16 slots, Block B: 16 slots, Block C: 16 slots)
        List<YardSlot> allSlots = new ArrayList<>();
        YardSlot.YardBlock[] blocks = {YardSlot.YardBlock.BLOCK_A, YardSlot.YardBlock.BLOCK_B, YardSlot.YardBlock.BLOCK_C};

        for (YardSlot.YardBlock blk : blocks) {
            String prefix = blk == YardSlot.YardBlock.BLOCK_A ? "A" : (blk == YardSlot.YardBlock.BLOCK_B ? "B" : "C");
            for (int i = 1; i <= 16; i++) {
                String code = String.format("%s-%02d", prefix, i);
                YardSlot slot = YardSlot.builder()
                        .block(blk)
                        .slotCode(code)
                        .slotIndex(i)
                        .build();
                allSlots.add(slot);
            }
        }
        allSlots = yardSlotRepository.saveAll(allSlots);

        // 6. Tạo Containers mẫu (Đa dạng DRY/REEFER, FULL/EMPTY, GOOD/DAMAGED)
        List<Container> initialContainers = List.of(
                Container.builder().containerNumber("MSKU9182736").isoType("40HC").containerType(Container.ContainerType.DRY).cargoStatus(Container.CargoStatus.FULL).condition(Container.Condition.GOOD).shippingLine(maersk).sealNumber("SL-99881").grossWeightKg(28400.0).currentStatus(Container.TrackingStatus.IN_YARD).build(),
                Container.builder().containerNumber("MSKU3451290").isoType("20GP").containerType(Container.ContainerType.REEFER).cargoStatus(Container.CargoStatus.FULL).condition(Container.Condition.GOOD).shippingLine(maersk).sealNumber("SL-99882").grossWeightKg(22100.0).currentStatus(Container.TrackingStatus.IN_YARD).build(),
                Container.builder().containerNumber("MSCU1122334").isoType("40HC").containerType(Container.ContainerType.DRY).cargoStatus(Container.CargoStatus.EMPTY).condition(Container.Condition.GOOD).shippingLine(msc).sealNumber("SL-77661").grossWeightKg(3800.0).currentStatus(Container.TrackingStatus.IN_YARD).build(),
                Container.builder().containerNumber("MSCU5566778").isoType("40HC").containerType(Container.ContainerType.REEFER).cargoStatus(Container.CargoStatus.FULL).condition(Container.Condition.DAMAGED).shippingLine(msc).sealNumber("SL-77662").grossWeightKg(29100.0).currentStatus(Container.TrackingStatus.IN_YARD).build(),
                Container.builder().containerNumber("CMAU9988776").isoType("20GP").containerType(Container.ContainerType.DRY).cargoStatus(Container.CargoStatus.FULL).condition(Container.Condition.GOOD).shippingLine(cma).sealNumber("SL-55441").grossWeightKg(21500.0).currentStatus(Container.TrackingStatus.IN_YARD).build(),
                Container.builder().containerNumber("CMAU3344556").isoType("45HC").containerType(Container.ContainerType.DRY).cargoStatus(Container.CargoStatus.EMPTY).condition(Container.Condition.GOOD).shippingLine(cma).sealNumber("SL-55442").grossWeightKg(4200.0).currentStatus(Container.TrackingStatus.IN_YARD).build(),
                Container.builder().containerNumber("MSKU7711223").isoType("40HC").containerType(Container.ContainerType.DRY).cargoStatus(Container.CargoStatus.FULL).condition(Container.Condition.GOOD).shippingLine(maersk).sealNumber("SL-99883").grossWeightKg(27800.0).currentStatus(Container.TrackingStatus.GATE_IN).build(),
                Container.builder().containerNumber("MSCU9900112").isoType("20GP").containerType(Container.ContainerType.REEFER).cargoStatus(Container.CargoStatus.EMPTY).condition(Container.Condition.GOOD).shippingLine(msc).sealNumber("SL-77663").grossWeightKg(2900.0).currentStatus(Container.TrackingStatus.EXPECTED).build()
        );

        List<Container> savedContainers = containerRepository.saveAll(initialContainers);

        // Gán 6 container vào các ô bãi
        int[] slotIndicesToOccupy = {0, 2, 4, 16, 18, 32}; // A-01, A-03, A-05, B-01, B-03, C-01
        for (int i = 0; i < 6; i++) {
            YardSlot slot = allSlots.get(slotIndicesToOccupy[i]);
            Container cont = savedContainers.get(i);
            slot.setContainer(cont);
            yardSlotRepository.save(slot);

            // Ghi nhận lịch sử xếp bãi
            movementHistoryRepository.save(MovementHistory.builder()
                    .container(cont)
                    .eventType(MovementHistory.MovementType.ASSIGN_SLOT)
                    .fromLocation("Cổng vào (Gate-01)")
                    .toLocation("Ô bãi " + slot.getSlotCode() + " (" + slot.getBlock().getDisplayName() + ")")
                    .operatorName("Trần Điều Hành")
                    .timestamp(LocalDateTime.now().minusHours(4 - i))
                    .notes("Hạ bãi phân bổ vị trí")
                    .build());
        }

        // 7. Tạo Appointment cho tài xế
        appointmentRepository.save(Appointment.builder()
                .driver(driver)
                .appointmentType(Appointment.AppointmentType.IMPORT_PICKUP)
                .bookingNumber("BK-IMP-2026-001")
                .containerNumber(savedContainers.get(0).getContainerNumber())
                .container(savedContainers.get(0))
                .licensePlate("51C-889.92")
                .driverPhone("0904567890")
                .appointmentTime(LocalDateTime.now().plusHours(3))
                .sealNumber(savedContainers.get(0).getSealNumber())
                .status(Appointment.AppointmentStatus.CONFIRMED)
                .notes("Xe tải hẹn đến lấy cont nhập khẩu số MSKU9182736")
                .build());

        appointmentRepository.save(Appointment.builder()
                .driver(driver)
                .appointmentType(Appointment.AppointmentType.EXPORT_DROPOFF)
                .bookingNumber("BK-EXP-2026-002")
                .containerNumber("MSKU7711223")
                .container(savedContainers.get(6))
                .licensePlate("51D-123.45")
                .driverPhone("0904567890")
                .appointmentTime(LocalDateTime.now().minusMinutes(30))
                .sealNumber("SL-99883")
                .status(Appointment.AppointmentStatus.CHECKED_IN)
                .notes("Hạ cont xuất khẩu đi tàu MSC Oscar")
                .build());

        log.info("Khởi tạo dữ liệu mẫu hoàn tất!");
    }
}
