package com.ctms.controller;

import com.ctms.model.Container;
import com.ctms.service.AppointmentService;
import com.ctms.service.ContainerService;
import com.ctms.service.GateService;
import com.ctms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gate")
public class GateController {

    private final GateService gateService;
    private final AppointmentService appointmentService;
    private final ContainerService containerService;
    private final UserService userService;

    public GateController(GateService gateService, AppointmentService appointmentService,
                          ContainerService containerService, UserService userService) {
        this.gateService = gateService;
        this.appointmentService = appointmentService;
        this.containerService = containerService;
        this.userService = userService;
    }

    @GetMapping
    public String gateHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "gate/index";
    }

    /**
     * Use Case: Confirm Gate In
     */
    @GetMapping("/in")
    public String gateInForm(@RequestParam(value = "booking", required = false) String booking,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("bookingParam", booking);
        if (booking != null && !booking.trim().isEmpty()) {
            appointmentService.findByBookingNumber(booking.trim()).ifPresent(app -> {
                model.addAttribute("app", app);
            });
        }
        return "gate/gate-in";
    }

    @PostMapping("/in")
    public String processGateIn(@RequestParam("containerNumber") String containerNumber,
                                @RequestParam("sealNumber") String sealNumber,
                                @RequestParam(value = "containerType", defaultValue = "DRY") Container.ContainerType containerType,
                                @RequestParam(value = "cargoStatus", defaultValue = "FULL") Container.CargoStatus cargoStatus,
                                @RequestParam(value = "condition", defaultValue = "GOOD") Container.Condition condition,
                                @RequestParam(value = "bookingNumber", required = false) String bookingNumber,
                                @RequestParam("licensePlate") String licensePlate,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            String operatorName = userDetails != null ? userDetails.getUsername() : "Gate Operator";
            gateService.confirmGateIn(containerNumber, sealNumber, containerType, cargoStatus, condition, bookingNumber, licensePlate, operatorName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận xe " + licensePlate + " mang cont " + containerNumber + " vào cổng (Gate In) thành công! Nhật ký di chuyển đã được ghi nhận tự động.");
            return "redirect:/gate";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi Gate In: " + com.ctms.util.SafeMessage.of(e));
            return "redirect:/gate/in";
        }
    }

    /**
     * Use Case: Confirm Gate Out
     */
    @GetMapping("/out")
    public String gateOutForm(@RequestParam(value = "container", required = false) String container,
                             @RequestParam(value = "plate", required = false) String plate,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("inYardContainers", containerService.findAll().stream()
                .filter(c -> c.getCurrentStatus() == Container.TrackingStatus.IN_YARD)
                .toList());
        model.addAttribute("selectedContainer", container != null ? container.trim().toUpperCase() : null);
        model.addAttribute("selectedPlate", plate != null ? plate.trim().toUpperCase() : null);
        return "gate/gate-out";
    }

    @PostMapping("/out")
    public String processGateOut(@RequestParam("containerNumber") String containerNumber,
                                 @RequestParam("sealNumber") String sealNumber,
                                 @RequestParam(value = "condition", defaultValue = "GOOD") Container.Condition condition,
                                 @RequestParam("licensePlate") String licensePlate,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            String operatorName = userDetails != null ? userDetails.getUsername() : "Gate Operator";
            gateService.confirmGateOut(containerNumber, sealNumber, condition, licensePlate, operatorName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận xe " + licensePlate + " chở cont " + containerNumber + " xuất bãi (Gate Out) thành công!");
            return "redirect:/gate";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi Gate Out: " + com.ctms.util.SafeMessage.of(e));
            return "redirect:/gate/out";
        }
    }
}
