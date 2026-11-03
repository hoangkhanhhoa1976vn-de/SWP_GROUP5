package com.ctms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chỉ hiển thị cho người dùng các lỗi nghiệp vụ có chủ đích
 * (IllegalArgumentException / IllegalStateException). Các lỗi khác (SQL, NPE...)
 * được thay bằng thông báo chung để không lộ chi tiết nội bộ.
 */
public final class SafeMessage {

    private static final Logger log = LoggerFactory.getLogger(SafeMessage.class);

    private SafeMessage() {
    }

    public static String of(Throwable e) {
        if ((e instanceof IllegalArgumentException || e instanceof IllegalStateException)
                && e.getMessage() != null) {
            return e.getMessage();
        }
        log.error("Lỗi không mong đợi", e);
        return "Đã xảy ra lỗi hệ thống. Vui lòng thử lại hoặc liên hệ quản trị viên.";
    }
}
