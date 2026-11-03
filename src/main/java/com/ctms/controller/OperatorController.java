package com.ctms.controller;

import com.ctms.model.Container;
import com.ctms.model.User;
import com.ctms.model.Vessel;
import com.ctms.model.YardSlot;
import com.ctms.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Luồng 2: Phân hệ Điều hành Hiện trường (Operator Portal - /operator/**)
 * Dành riêng cho Actor: Port Operator (Điều phối viên Cổng, Bãi & Cầu bến)
 * Nghiệp vụ: Trực ban điều phối xe ra/vào cổng, duyệt lệnh check-in/out, gán và đảo bãi container 48 slot, phân bổ cầu bến tàu.
 */
@Controller
@RequestMapping("/operator")
public class OperatorController {

    private final DashboardService dashboardService;
    private final YardService yardService;
    private final GateService gateService;
    private final BerthService berthService;
    private final ContainerService containerService;
    private final AppointmentService appointmentService;
    private final UserService userService;

    public OperatorController(DashboardService dashboardService,
                              YardService yardService,
                              GateService gateService,
                              BerthService berthService,
                              ContainerService containerService,
                              AppointmentService appointmentService,
                              UserService userService) {
        this.dashboardService = dashboardService;
        this.yardService = yardService;
        this.gateService = gateService;
        this.berthService = berthService;
        this.containerService = containerService;
        this.appointmentService = appointmentService;
        this.userService = userService;
    }

    /**
     * Bảng điều khiển tác nghiệp Hiện trường
     */
    @GetMapping({"", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("stats", dashboardService.getStats());

        // Bãi: số slot đã chiếm và còn trống
        model.addAttribute("occupiedCount", yardService.getOccupiedCount());
        model.addAttribute("totalCount", yardService.getTotalCount());

        // Cổng: danh sách lịch hẹn chờ duyệt
        model.addAttribute("appointments", appointmentService.getAllAppointments());

        // Cầu bến: tàu đang neo đậu
        model.addAttribute("berthedVessels", berthService.getAllVessels().stream()
                .filter(v -> v.getStatus() == Vessel.VesselStatus.BERTHED)
                .toList());

        // Container chờ xếp bãi
        List<Container> waitingContainers = containerService.findAll().stream()
                .filter(c -> c.getCurrentStatus() == Container.TrackingStatus.GATE_IN || c.getCurrentSlot() == null)
                .toList();
        model.addAttribute("waitingContainers", waitingContainers);

        return "operator/dashboard";
    }

    /**
     * Sơ đồ Bãi Container 48 Slots
     */
    @GetMapping("/yard")
    public String yardOverview(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);

        model.addAttribute("blockASlots", yardService.getSlotsByBlock(YardSlot.YardBlock.BLOCK_A));
        model.addAttribute("blockBSlots", yardService.getSlotsByBlock(YardSlot.YardBlock.BLOCK_B));
        model.addAttribute("blockCSlots", yardService.getSlotsByBlock(YardSlot.YardBlock.BLOCK_C));
        model.addAttribute("occupiedCount", yardService.getOccupiedCount());
        model.addAttribute("totalCount", yardService.getTotalCount());

        List<Container> waitingContainers = containerService.findAll().stream()
                .filter(c -> c.getCurrentStatus() == Container.TrackingStatus.GATE_IN || c.getCurrentSlot() == null)
                .toList();
        model.addAttribute("waitingContainers", waitingContainers);

        model.addAttribute("emptySlots", yardService.getEmptySlots());
        model.addAttribute("allSlots", yardService.getAllSlots());

        return "yard/index";
    }

    /**
     * Điều hành Cổng Kiểm Soát
     */
    @GetMapping("/gate")
    public String gate(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "gate/index";
    }

    /**
     * Cổng Vào (Gate In)
     */
    @GetMapping("/gate/in")
    public String gateIn(@RequestParam(value = "booking", required = false) String booking,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("bookingParam", booking);
        if (booking != null && !booking.trim().isEmpty()) {
            appointmentService.findByBookingNumber(booking.trim()).ifPresent(app -> {
                model.addAttribute("app", app);
            });
        }
        return "gate/gate-in";
    }

    /**
     * Cổng Ra (Gate Out)
     */
    @GetMapping("/gate/out")
    public String gateOut(@RequestParam(value = "container", required = false) String container,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("containerParam", container);
        if (container != null && !container.trim().isEmpty()) {
            containerService.findByContainerNumber(container.trim()).ifPresent(c -> {
                model.addAttribute("cont", c);
            });
        }
        return "gate/gate-out";
    }

    /**
     * Trực ban Cầu bến
     */
    @GetMapping("/berths")
    public String berths(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("berths", berthService.getAllBerths());
        model.addAttribute("vessels", berthService.getAllVessels());
        model.addAttribute("scheduledVessels", berthService.getAllVessels().stream()
                .filter(v -> v.getStatus() == Vessel.VesselStatus.SCHEDULED)
                .toList());
        return "vessel/berth-overview";
    }
}
