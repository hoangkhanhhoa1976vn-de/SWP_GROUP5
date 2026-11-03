package com.ctms.controller;

import com.ctms.model.Role;
import com.ctms.model.User;
import com.ctms.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserService userService;

    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Dispatcher thông minh: Phân luồng người dùng về đúng Portal theo Role
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername());
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (currentUser.getRole() == Role.ROLE_MANAGER) {
            return "redirect:/manager/dashboard";
        } else if (currentUser.getRole() == Role.ROLE_OPERATOR) {
            return "redirect:/operator/dashboard";
        } else if (currentUser.getRole() == Role.ROLE_SHIPPING_LINE) {
            return "redirect:/portal/shipping-line";
        } else if (currentUser.getRole() == Role.ROLE_TRUCK_DRIVER) {
            return "redirect:/portal/driver";
        }

        return "redirect:/manager/dashboard";
    }
}
