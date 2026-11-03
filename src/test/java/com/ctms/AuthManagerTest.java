package com.ctms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthManagerTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void testAuth() {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken("manager", "test-only-password")
            );
            System.out.println("AUTH RESULT: " + auth.isAuthenticated());
            assertTrue(auth.isAuthenticated());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

