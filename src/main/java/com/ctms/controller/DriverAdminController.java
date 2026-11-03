package com.ctms.controller;

import com.ctms.model.Role;
import com.ctms.model.User;
import com.ctms.repository.UserRepository;
import com.ctms.service.UserService;
import com.ctms.util.SafeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverAdminController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public String listDrivers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("drivers", userRepository.findByRole(Role.ROLE_TRUCK_DRIVER));
        return "admin/drivers";
    }

    @PostMapping("/create")
    public String createDriver(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam("fullName") String fullName,
                               @RequestParam("phone") String phone,
                               @RequestParam("licensePlate") String licensePlate,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.createDriverAccount(username, password, fullName, phone, licensePlate);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cấp tài khoản tài xế " + fullName + " (" + username + ") thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cấp tài khoản tài xế: " + SafeMessage.of(e));
        }
        return "redirect:/drivers";
    }
}

