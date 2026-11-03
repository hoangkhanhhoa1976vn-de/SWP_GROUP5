package com.ctms.service;

import com.ctms.model.*;
import com.ctms.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CtmsServicesTest {

    @Autowired
    private GateService gateService;

    @Autowired
    private YardService yardService;

    @Autowired
    private BerthService berthService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private YardSlotRepository yardSlotRepository;

    @Autowired
    private BerthRepository berthRepository;

    @Autowired
    private VesselRepository vesselRepository;

    @Autowired
    private ShippingLineRepository shippingLineRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Gate In: Thành công khởi tạo container mới và tự động gán hãng tàu + thời gian vào cổng")
    void testGateInNewContainer() {
        String testContNo = "MSKU8889990";
        Container cont = gateService.confirmGateIn(
                testContNo,
                "SEAL-12345",
                Container.ContainerType.DRY,
                Container.CargoStatus.FULL,
                Container.Condition.GOOD,
                null,
                "51D-999.88",
                "Gate Operator Test"
        );

        assertNotNull(cont);
        assertEquals(testContNo, cont.getContainerNumber());
        assertEquals(Container.TrackingStatus.GATE_IN, cont.getCurrentStatus());
        assertNotNull(cont.getShippingLine(), "Hãng tàu phải được tự động gán");
        assertNotNull(cont.getGateInTime(), "Thời gian vào cổng phải được ghi nhận");
        assertEquals("SEAL-12345", cont.getSealNumber());
    }

    @Test
    @DisplayName("Gate In: Từ chối nếu thiếu số niêm chì hoặc biển số xe")
    void testGateInValidationFails() {
        assertThrows(IllegalArgumentException.class, () ->
                gateService.confirmGateIn("MSKU1112223", "", Container.ContainerType.DRY,
                        Container.CargoStatus.FULL, Container.Condition.GOOD, null, "51C-12345", "Op")
        );

        assertThrows(IllegalArgumentException.class, () ->
                gateService.confirmGateIn("MSKU1112223", "SEAL-1", Container.ContainerType.DRY,
                        Container.CargoStatus.FULL, Container.Condition.GOOD, null, "", "Op")
        );
    }

    @Test
    @DisplayName("Yard: Gán container vào ô bãi thành công và cập nhật trạng thái IN_YARD")
    void testAssignContainerToSlot() {
        List<Container> waiting = containerRepository.findByCurrentStatus(Container.TrackingStatus.GATE_IN);
        assertFalse(waiting.isEmpty(), "Cần có cont GATE_IN từ dữ liệu mẫu");
        Container targetCont = waiting.get(0);

        List<YardSlot> emptySlots = yardService.getEmptySlots();
        assertFalse(emptySlots.isEmpty(), "Phải còn ô bãi trống");
        YardSlot slot = emptySlots.get(0);

        yardService.assignContainerToSlot(targetCont.getId(), slot.getId(), "Test Operator");

        Container updated = containerRepository.findById(targetCont.getId()).orElseThrow();
        assertEquals(Container.TrackingStatus.IN_YARD, updated.getCurrentStatus());

        YardSlot updatedSlot = yardSlotRepository.findById(slot.getId()).orElseThrow();
        assertTrue(updatedSlot.isOccupied());
        assertEquals(targetCont.getId(), updatedSlot.getContainer().getId());
    }

    @Test
    @DisplayName("Yard: Đảo chuyển container giữa 2 ô bãi thành công")
    void testMoveContainerBetweenSlots() {
        YardSlot fromSlot = yardSlotRepository.findAll().stream()
                .filter(YardSlot::isOccupied)
                .findFirst()
                .orElseThrow();
        Long containerId = fromSlot.getContainer().getId();

        YardSlot toSlot = yardSlotRepository.findAll().stream()
                .filter(s -> !s.isOccupied())
                .findFirst()
                .orElseThrow();

        yardService.moveContainerBetweenSlots(fromSlot.getId(), toSlot.getId(), "Test Operator");

        YardSlot reloadedFrom = yardSlotRepository.findById(fromSlot.getId()).orElseThrow();
        YardSlot reloadedTo = yardSlotRepository.findById(toSlot.getId()).orElseThrow();

        assertFalse(reloadedFrom.isOccupied());
        assertTrue(reloadedTo.isOccupied());
        assertEquals(containerId, reloadedTo.getContainer().getId());
    }

    @Test
    @DisplayName("Berths: Phân bổ tàu vào cầu bến và giải phóng khi tàu rời bến")
    void testAssignAndDepartVessel() {
        Berth availableBerth = berthRepository.findAll().stream()
                .filter(b -> b.getStatus() == Berth.BerthStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();

        Vessel scheduledVessel = vesselRepository.findAll().stream()
                .filter(v -> v.getStatus() == Vessel.VesselStatus.SCHEDULED)
                .findFirst()
                .orElseThrow();

        berthService.assignVesselToBerth(scheduledVessel.getId(), availableBerth.getId());

        Berth updatedBerth = berthRepository.findById(availableBerth.getId()).orElseThrow();
        Vessel updatedVessel = vesselRepository.findById(scheduledVessel.getId()).orElseThrow();

        assertEquals(Berth.BerthStatus.OCCUPIED, updatedBerth.getStatus());
        assertEquals(scheduledVessel.getId(), updatedBerth.getCurrentVessel().getId());
        assertEquals(Vessel.VesselStatus.BERTHED, updatedVessel.getStatus());

        // Depart vessel
        berthService.departVessel(scheduledVessel.getId());

        Berth departedBerth = berthRepository.findById(availableBerth.getId()).orElseThrow();
        Vessel departedVessel = vesselRepository.findById(scheduledVessel.getId()).orElseThrow();

        assertEquals(Berth.BerthStatus.AVAILABLE, departedBerth.getStatus());
        assertNull(departedBerth.getCurrentVessel());
        assertEquals(Vessel.VesselStatus.DEPARTED, departedVessel.getStatus());
    }

    @Test
    @DisplayName("Appointments: Đặt lịch hẹn lấy cont nhập khẩu và hạ cont xuất khẩu")
    void testCreateAppointments() {
        User driver = userRepository.findByUsername("driver").orElseThrow();

        // 1. Hạ hàng xuất
        String expBooking = "BK-EXP-TEST-999";
        Appointment expApp = appointmentService.createAppointment(
                driver,
                Appointment.AppointmentType.EXPORT_DROPOFF,
                expBooking,
                "MSKU8881112",
                "51D-998.81",
                "0904567890",
                LocalDateTime.now().plusHours(5),
                "SL-TEST-01",
                "Ghi chú test xuất"
        );
        assertNotNull(expApp);
        assertEquals(expBooking, expApp.getBookingNumber());
        assertEquals(Appointment.AppointmentStatus.CONFIRMED, expApp.getStatus());

        // 2. Lấy hàng nhập (chọn 1 cont đang IN_YARD và chưa có lịch hẹn hoạt động)
        Container inYardCont = containerRepository.findAll().stream()
                .filter(c -> c.getCurrentStatus() == Container.TrackingStatus.IN_YARD
                        && !appointmentRepository.existsByContainerNumberAndStatusIn(c.getContainerNumber(),
                                java.util.EnumSet.of(Appointment.AppointmentStatus.PENDING,
                                                     Appointment.AppointmentStatus.CONFIRMED,
                                                     Appointment.AppointmentStatus.CHECKED_IN)))
                .findFirst()
                .orElseThrow();

        String impBooking = "BK-IMP-TEST-888";
        Appointment impApp = appointmentService.createAppointment(
                driver,
                Appointment.AppointmentType.IMPORT_PICKUP,
                impBooking,
                inYardCont.getContainerNumber(),
                "51D-998.81",
                "0904567890",
                LocalDateTime.now().plusHours(6),
                inYardCont.getSealNumber(),
                "Ghi chú test nhập"
        );
        assertNotNull(impApp);
        assertEquals(impBooking, impApp.getBookingNumber());
    }

    @Test
    @DisplayName("Users: Cấp tài khoản tài xế mới và đăng nhập bằng SĐT")
    void testCreateDriverAccount() {
        String driverUser = "driver_new_01";
        String driverPhone = "0987654321";

        User newDriver = userService.createDriverAccount(
                driverUser,
                "pass12345",
                "Tài Xế Mới Cấp",
                driverPhone,
                "51G-556.77"
        );

        assertNotNull(newDriver);
        assertEquals(Role.ROLE_TRUCK_DRIVER, newDriver.getRole());

        // Tìm bằng SĐT
        User foundByPhone = userService.findByUsername(driverPhone);
        assertNotNull(foundByPhone);
        assertEquals(driverUser, foundByPhone.getUsername());
    }
}
