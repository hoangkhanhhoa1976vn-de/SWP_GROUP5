package com.ctms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý khi truy cập trái phân quyền:
 * Thay vì trang 403 xấu xí, tự động đưa người dùng về đúng Phân hệ của họ
 * kèm thông báo hướng dẫn rõ ràng.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        Authentication auth = (Authentication) request.getUserPrincipal();

        String redirectUrl = "/login";
        String message = "Bạn không có quyền truy cập vào phân hệ này!";

        if (auth != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String role = ga.getAuthority();
                if ("ROLE_MANAGER".equals(role)) {
                    redirectUrl = "/manager/dashboard";
                    break;
                } else if ("ROLE_OPERATOR".equals(role)) {
                    redirectUrl = "/operator/dashboard";
                    message = "Tài khoản Operator chỉ được thao tác tại phân hệ Hiện trường (Bãi, Cổng, Bến).";
                    break;
                } else if ("ROLE_SHIPPING_LINE".equals(role)) {
                    redirectUrl = "/portal/shipping-line";
                    message = "Tài khoản Hãng tàu chỉ được thao tác tại Cổng Dịch Vụ Hãng Tàu.";
                    break;
                } else if ("ROLE_TRUCK_DRIVER".equals(role)) {
                    redirectUrl = "/portal/driver";
                    message = "Tài khoản Tài xế chỉ được thao tác tại Cổng Đặt Lịch & QR Xe Tải.";
                    break;
                }
            }
        }

        session.setAttribute("deniedMessage", message);
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
