package com.ctms.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý định tuyến tự động về đúng Luồng chuyên biệt ngay sau khi đăng nhập:
 * 1. Manager -> /manager/dashboard
 * 2. Operator -> /operator/dashboard
 * 3. Shipping Line -> /portal/shipping-line
 * 4. Truck Driver -> /portal/driver
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        SecurityContext sc = SecurityContextHolder.getContext();

        String targetUrl = "/dashboard";

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_MANAGER".equals(role)) {
                session.setAttribute(MultiRoleSecurityContextRepository.KEY_MANAGER, sc);
                targetUrl = "/manager/dashboard";
                break;
            } else if ("ROLE_OPERATOR".equals(role)) {
                session.setAttribute(MultiRoleSecurityContextRepository.KEY_OPERATOR, sc);
                targetUrl = "/operator/dashboard";
                break;
            } else if ("ROLE_SHIPPING_LINE".equals(role)) {
                session.setAttribute(MultiRoleSecurityContextRepository.KEY_SHIPPING, sc);
                targetUrl = "/portal/shipping-line";
                break;
            } else if ("ROLE_TRUCK_DRIVER".equals(role)) {
                session.setAttribute(MultiRoleSecurityContextRepository.KEY_DRIVER, sc);
                targetUrl = "/portal/driver";
                break;
            }
        }

        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}
