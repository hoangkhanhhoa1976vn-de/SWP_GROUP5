package com.ctms.controller;

import com.ctms.config.MultiRoleSecurityContextRepository;
import com.ctms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Bạn đã đăng xuất thành công!");
        }

        // Lấy thông báo từ chối truy cập (nếu có từ AccessDeniedHandler)
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("deniedMessage") != null) {
            model.addAttribute("errorMessage", session.getAttribute("deniedMessage"));
            session.removeAttribute("deniedMessage");
        }

        return "auth/login";
    }

    /**
     * Tiện ích đăng nhập nhanh 1-Click phục vụ kiểm thử và chấm đồ án:
     * Lưu context vào session riêng của từng vai trò, giúp mở 4 tab độc lập
     * trên cùng một trình duyệt mà không bị đá nhau!
     */
    @GetMapping("/quick-login")
    public String quickLogin(@RequestParam("user") String username, HttpServletRequest request) {
        UserDetails userDetails = userService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        if ("manager".equals(username)) {
            session.setAttribute(MultiRoleSecurityContextRepository.KEY_MANAGER, sc);
            return "redirect:/manager/dashboard";
        } else if ("operator".equals(username)) {
            session.setAttribute(MultiRoleSecurityContextRepository.KEY_OPERATOR, sc);
            return "redirect:/operator/dashboard";
        } else if ("shipping_maersk".equals(username)) {
            session.setAttribute(MultiRoleSecurityContextRepository.KEY_SHIPPING, sc);
            return "redirect:/portal/shipping-line";
        } else if ("driver".equals(username)) {
            session.setAttribute(MultiRoleSecurityContextRepository.KEY_DRIVER, sc);
            return "redirect:/portal/driver";
        }

        return "redirect:/dashboard";
    }

    /**
     * Đăng xuất độc lập từng vai trò (không làm mất session của các vai trò khác đang mở ở tab khác)
     */
    @GetMapping("/role-logout")
    public String roleLogout(@RequestParam("role") String role, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            switch (role.toLowerCase()) {
                case "manager" -> session.removeAttribute(MultiRoleSecurityContextRepository.KEY_MANAGER);
                case "operator" -> session.removeAttribute(MultiRoleSecurityContextRepository.KEY_OPERATOR);
                case "shipping" -> session.removeAttribute(MultiRoleSecurityContextRepository.KEY_SHIPPING);
                case "driver" -> session.removeAttribute(MultiRoleSecurityContextRepository.KEY_DRIVER);
                default -> session.invalidate();
            }
        }
        return "redirect:/login?logout=" + role;
    }
}
