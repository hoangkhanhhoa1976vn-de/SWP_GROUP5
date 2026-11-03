package com.ctms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Quản lý Session Context độc lập cho 4 Luồng / 4 Actor:
 * 1. Manager (/manager/**)
 * 2. Operator (/operator/**, /yard/**, /gate/**)
 * 3. Shipping Line (/portal/shipping-line/**)
 * 4. Truck Driver (/portal/driver/**)
 *
 * Giúp người dùng có thể mở 4 Tab hoặc 4 Cửa sổ cùng lúc trên 1 trình duyệt
 * mà không bao giờ bị đè Session hay đá nhau ra ngoài!
 */
public class MultiRoleSecurityContextRepository implements SecurityContextRepository {

    public static final String KEY_MANAGER = "SPRING_SECURITY_CONTEXT_MANAGER";
    public static final String KEY_OPERATOR = "SPRING_SECURITY_CONTEXT_OPERATOR";
    public static final String KEY_SHIPPING = "SPRING_SECURITY_CONTEXT_SHIPPING";
    public static final String KEY_DRIVER = "SPRING_SECURITY_CONTEXT_DRIVER";
    public static final String KEY_DEFAULT = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    public static String getSessionKeyForUri(String uri) {
        if (uri == null) return KEY_DEFAULT;
        if (uri.startsWith("/manager")) return KEY_MANAGER;
        if (uri.startsWith("/operator") || uri.startsWith("/yard") || uri.startsWith("/gate")) return KEY_OPERATOR;
        if (uri.startsWith("/portal/shipping-line")) return KEY_SHIPPING;
        if (uri.startsWith("/portal/driver")) return KEY_DRIVER;
        return KEY_DEFAULT;
    }

    public static String getSessionKeyForAuthority(String authority) {
        if ("ROLE_MANAGER".equals(authority)) return KEY_MANAGER;
        if ("ROLE_OPERATOR".equals(authority)) return KEY_OPERATOR;
        if ("ROLE_SHIPPING_LINE".equals(authority)) return KEY_SHIPPING;
        if ("ROLE_TRUCK_DRIVER".equals(authority)) return KEY_DRIVER;
        return KEY_DEFAULT;
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        return new DeferredSecurityContext() {
            private SecurityContext context;
            private boolean isGenerated;

            @Override
            public SecurityContext get() {
                if (this.context == null) {
                    this.context = readContextFromSession(request);
                    this.isGenerated = (this.context == null);
                    if (this.isGenerated) {
                        this.context = SecurityContextHolder.createEmptyContext();
                    }
                }
                return this.context;
            }

            @Override
            public boolean isGenerated() {
                get();
                return this.isGenerated;
            }
        };
    }

    private SecurityContext readContextFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;

        String uri = request.getRequestURI();
        String key = getSessionKeyForUri(uri);

        Object roleCtx = session.getAttribute(key);
        if (roleCtx instanceof SecurityContext sc && isValidContext(sc)) {
            return sc;
        }

        // Fallback: Nếu truy cập các trang dùng chung (/dashboard, static, etc.)
        Object defaultCtx = session.getAttribute(KEY_DEFAULT);
        if (defaultCtx instanceof SecurityContext sc && isValidContext(sc)) {
            return sc;
        }

        // Nếu là trang chung mà có bất kỳ role nào đã đăng nhập, ưu tiên role phù hợp
        if (key.equals(KEY_DEFAULT)) {
            for (String k : new String[]{KEY_MANAGER, KEY_OPERATOR, KEY_SHIPPING, KEY_DRIVER}) {
                Object anyCtx = session.getAttribute(k);
                if (anyCtx instanceof SecurityContext sc && isValidContext(sc)) {
                    return sc;
                }
            }
        }

        return null;
    }

    private boolean isValidContext(SecurityContext sc) {
        return sc != null && sc.getAuthentication() != null && sc.getAuthentication().isAuthenticated();
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String key = getSessionKeyForUri(uri);

        HttpSession session = request.getSession(true);
        if (context == null || context.getAuthentication() == null) {
            session.removeAttribute(key);
            if (key.equals(KEY_DEFAULT)) {
                session.removeAttribute(KEY_DEFAULT);
            }
        } else {
            session.setAttribute(key, context);
            // Cũng lưu key mặc định để các thư viện Spring Security & Thymeleaf tiện đọc
            session.setAttribute(KEY_DEFAULT, context);

            // Đồng bộ thêm theo Role của User
            Authentication auth = context.getAuthentication();
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String roleKey = getSessionKeyForAuthority(ga.getAuthority());
                if (!roleKey.equals(KEY_DEFAULT)) {
                    session.setAttribute(roleKey, context);
                }
            }
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        String uri = request.getRequestURI();
        String key = getSessionKeyForUri(uri);
        return session.getAttribute(key) != null || session.getAttribute(KEY_DEFAULT) != null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext ctx = readContextFromSession(requestResponseHolder.getRequest());
        return ctx != null ? ctx : SecurityContextHolder.createEmptyContext();
    }
}
