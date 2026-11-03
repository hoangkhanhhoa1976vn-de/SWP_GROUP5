package com.ctms.controller;

import com.ctms.model.Container;
import com.ctms.model.User;
import com.ctms.model.Vessel;
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
 * Luồng 1: Phân hệ Quản lý Cảng (Manager Portal - /manager/**)
 * Dành riêng cho Actor: Port Manager (Quản lý cấp cao)
 * Nghiệp vụ: Xem KPI tổng quan, phân tích hiệu suất bãi/cầu bến, quản trị hãng tàu đối tác, báo cáo doanh thu & sản lượng.
 */
@Controller
@RequestMapping("/manager")
public class ManagerController {

    private final DashboardService dashboardService;
    private final ShippingLineService shippingLineService;
    private final BerthService berthService;
    private final ContainerService containerService;
    private final AppointmentService appointmentService;
    private final YardService yardService;
    private final UserService userService;

    public ManagerController(DashboardService dashboardService,
                             ShippingLineService shippingLineService,
                             BerthService berthService,
                             ContainerService containerService,
                             AppointmentService appointmentService,
                             YardService yardService,
                             UserService userService) {
        this.dashboardService = dashboardService;
        this.shippingLineService = shippingLineService;
        this.berthService = berthService;
        this.containerService = containerService;
        this.appointmentService = appointmentService;
        this.yardService = yardService;
        this.userService = userService;
    }

    /**
     * Dashboard Quản Lý Cảng
     */
    @GetMapping({"", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("shippingLines", shippingLineService.getAllShippingLines());
        model.addAttribute("berths", berthService.getAllBerths());
        model.addAttribute("vessels", berthService.getAllVessels());
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "manager/dashboard";
    }

    /**
     * Báo Cáo Hiệu Suất & Doanh Thu Cảng
     */
    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("allContainers", containerService.findAll());
        model.addAttribute("allAppointments", appointmentService.getAllAppointments());
        model.addAttribute("shippingLines", shippingLineService.getAllShippingLines());
        return "manager/reports";
    }

    /**
     * Quản lý Danh mục Hãng tàu (Tích hợp trong luồng Manager)
     */
    @GetMapping("/shipping-lines")
    public String shippingLines(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("shippingLines", shippingLineService.getAllShippingLines());
        return "vessel/shipping-lines";
    }

    /**
     * Quản lý Cầu bến & Lịch tàu cập cảng
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

    /**
     * Tra cứu & Giám sát Container toàn Cảng
     */
    @GetMapping("/containers")
    public String containers(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "containerType", required = false) Container.ContainerType containerType,
                             @RequestParam(value = "cargoStatus", required = false) Container.CargoStatus cargoStatus,
                             @RequestParam(value = "condition", required = false) Container.Condition condition,
                             @RequestParam(value = "currentStatus", required = false) Container.TrackingStatus currentStatus,
                             @RequestParam(value = "shippingLineId", required = false) Long shippingLineId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("containers", containerService.search(query, containerType, cargoStatus, condition, currentStatus, shippingLineId));
        model.addAttribute("shippingLines", shippingLineService.getAllShippingLines());
        model.addAttribute("paramQuery", query);
        model.addAttribute("paramType", containerType);
        model.addAttribute("paramCargo", cargoStatus);
        model.addAttribute("paramCondition", condition);
        model.addAttribute("paramStatus", currentStatus);
        model.addAttribute("paramShippingLineId", shippingLineId);
        return "container/list";
    }
}
