package com.ctms.controller;

import com.ctms.model.Appointment;
import com.ctms.model.Container;
import com.ctms.model.Role;
import com.ctms.model.User;
import com.ctms.model.Vessel;
import com.ctms.service.AppointmentService;
import com.ctms.service.BerthService;
import com.ctms.service.ContainerService;
import com.ctms.service.UserService;
import com.ctms.util.SafeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/portal")
@Slf4j
public class PortalController {

    private final UserService userService;
    private final ContainerService containerService;
    private final BerthService berthService;
    private final AppointmentService appointmentService;

    @Value("${ctms.fee.import-pickup:450000}")
    private int feeImportPickup;

    @Value("${ctms.fee.export-dropoff:400000}")
    private int feeExportDropoff;

    @Value("${ctms.payment.bank-code:MB}")
    private String bankCode;

    @Value("${ctms.payment.account-number:}")
    private String bankAccountNumber;

    @Value("${ctms.payment.account-name:CTMS PORT TERMINAL}")
    private String bankAccountName;

    public PortalController(UserService userService, ContainerService containerService,
                            BerthService berthService, AppointmentService appointmentService) {
        this.userService = userService;
        this.containerService = containerService;
        this.berthService = berthService;
        this.appointmentService = appointmentService;
    }

    /**
     * Use Case 4: View Own Shipping Info & View Vessel Schedule (Dành cho Shipping Line)
     * Tài khoản hãng tàu chỉ thấy dữ liệu của hãng mình; Manager (không thuộc hãng nào) thấy toàn bộ.
     */
    @GetMapping("/shipping-line")
    public String shippingLinePortal(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);

        List<Container> myContainers;
        List<Vessel> myVessels;
        if (currentUser != null && currentUser.getShippingLine() != null) {
            Long shippingLineId = currentUser.getShippingLine().getId();
            myContainers = containerService.findByShippingLine(shippingLineId);
            myVessels = berthService.getVesselsByShippingLine(shippingLineId);
        } else if (currentUser != null && currentUser.getRole() == Role.ROLE_MANAGER) {
            myContainers = containerService.findAll();
            myVessels = berthService.getAllVessels();
        } else {
            myContainers = List.of();
            myVessels = List.of();
        }

        model.addAttribute("myContainers", myContainers);
        model.addAttribute("myVessels", myVessels);
        model.addAttribute("allSchedules", berthService.getAllVessels());

        return "portal/shipping-line";
    }

    /**
     * Use Case 4: Book Appointment & View Gate Appointments (Dành cho Truck Driver)
     */
    @GetMapping("/driver")
    public String driverPortal(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);

        List<Appointment> myAppointments;
        if (currentUser != null && currentUser.getRole() == Role.ROLE_TRUCK_DRIVER) {
            myAppointments = appointmentService.getAppointmentsForDriver(currentUser.getId());
        } else {
            myAppointments = appointmentService.getAllAppointments();
        }
        model.addAttribute("myAppointments", myAppointments);
        model.addAttribute("bankCode", bankCode != null ? bankCode : "MB");
        model.addAttribute("bankAccountNumber", (bankAccountNumber != null && !bankAccountNumber.isBlank()) ? bankAccountNumber : "9999888868");
        model.addAttribute("bankAccountName", bankAccountName != null ? bankAccountName : "CTMS PORT TERMINAL");

        return "portal/truck-driver";
    }

    @PostMapping("/driver/book-appointment")
    public String bookAppointment(@RequestParam("appointmentType") Appointment.AppointmentType type,
                                  @RequestParam("bookingNumber") String bookingNumber,
                                  @RequestParam("containerNumber") String containerNumber,
                                  @RequestParam("licensePlate") String licensePlate,
                                  @RequestParam("phone") String phone,
                                  @RequestParam("appointmentTime") String appointmentTimeStr,
                                  @RequestParam(value = "sealNumber", required = false) String sealNumber,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
            if (currentUser == null) {
                throw new IllegalStateException("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại!");
            }

            LocalDateTime parsedTime;
            try {
                parsedTime = LocalDateTime.parse(appointmentTimeStr == null ? "" : appointmentTimeStr.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Thời gian hẹn không hợp lệ!");
            }

            String cleanBooking = bookingNumber == null ? "" : bookingNumber.trim().toUpperCase();
            String cleanContainer = containerNumber == null ? "" : containerNumber.trim().toUpperCase();
            String cleanPlate = licensePlate == null ? "" : licensePlate.trim().toUpperCase();
            String cleanPhone = phone == null ? "" : phone.trim();
            String cleanSeal = (sealNumber != null && !sealNumber.trim().isEmpty()) ? sealNumber.trim().toUpperCase() : null;
            String cleanNotes = (notes != null && !notes.trim().isEmpty()) ? notes.trim() : null;

            Appointment savedAppointment = appointmentService.createAppointment(
                    currentUser, type, cleanBooking, cleanContainer, cleanPlate, cleanPhone, parsedTime, cleanSeal, cleanNotes
            );

            String slotCode = (savedAppointment.getContainer() != null && savedAppointment.getContainer().getCurrentSlot() != null)
                    ? savedAppointment.getContainer().getCurrentSlot().getSlotCode() : "Chờ xếp bãi";

            int amount = (type == Appointment.AppointmentType.IMPORT_PICKUP) ? feeImportPickup : feeExportDropoff;
            String feeStr = NumberFormat.getInstance(Locale.GERMANY).format(amount); // 450.000

            String qrUrl = "";
            if (bankAccountNumber != null && !bankAccountNumber.isBlank()) {
                qrUrl = "https://img.vietqr.io/image/" + URLEncoder.encode(bankCode, StandardCharsets.UTF_8)
                        + "-" + URLEncoder.encode(bankAccountNumber, StandardCharsets.UTF_8) + "-compact2.png?amount=" + amount
                        + "&addInfo=" + URLEncoder.encode("CTMS " + savedAppointment.getBookingNumber(), StandardCharsets.UTF_8)
                        + "&accountName=" + URLEncoder.encode(bankAccountName, StandardCharsets.UTF_8);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký lịch hẹn (" + type.getDisplayName() + ") thành công! Vị trí bãi: " + slotCode);
            redirectAttributes.addFlashAttribute("newAppointment", savedAppointment);
            redirectAttributes.addFlashAttribute("assignedSlot", slotCode);
            redirectAttributes.addFlashAttribute("serviceFee", feeStr);
            redirectAttributes.addFlashAttribute("qrUrl", qrUrl);
        } catch (Exception e) {
            log.warn("Đặt lịch hẹn thất bại: {}", e.toString());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt lịch hẹn: " + SafeMessage.of(e));
        }
        return "redirect:/portal/driver";
    }
}
