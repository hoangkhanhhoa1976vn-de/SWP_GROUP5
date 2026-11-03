package com.ctms.controller;

import com.ctms.model.User;
import com.ctms.service.DashboardService;
import com.ctms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);

            // Chuyển hướng thông minh nếu là role chuyên biệt
            if (currentUser != null) {
                if (currentUser.getRole() == com.ctms.model.Role.ROLE_SHIPPING_LINE) {
                    return "redirect:/portal/shipping-line";
                } else if (currentUser.getRole() == com.ctms.model.Role.ROLE_TRUCK_DRIVER) {
                    return "redirect:/portal/driver";
                }
            }
        }

        model.addAttribute("stats", dashboardService.getStats());
        return "dashboard/index";
    }
}
