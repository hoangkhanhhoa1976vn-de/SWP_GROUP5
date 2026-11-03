package com.ctms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SafeMessageTest {

    @Test
    void businessErrorsAreShownAsIs() {
        assertEquals("Mã booking đã tồn tại!", SafeMessage.of(new IllegalArgumentException("Mã booking đã tồn tại!")));
        assertEquals("Ô bãi đã có container!", SafeMessage.of(new IllegalStateException("Ô bãi đã có container!")));
    }

    @Test
    void internalErrorsAreHidden() {
        String msg = SafeMessage.of(new RuntimeException("SQL: Invalid column name 'secret'"));
        assertFalse(msg.contains("SQL"));
        assertFalse(msg.contains("secret"));
    }
}
