package com.ctms.service;


import com.ctms.model.Role;
import com.ctms.model.User;
import com.ctms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Cho phép đăng nhập bằng cả Username hoặc Số điện thoại
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByPhone(username))
                .orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User createDriverAccount(String username, String password, String fullName, String phone, String licensePlate) {
        String cleanPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        if (cleanPhone != null && (userRepository.existsByPhone(cleanPhone) || userRepository.existsByUsername(cleanPhone))) {
            throw new IllegalArgumentException("Số điện thoại này đã được cấp tài khoản!");
        }

        User newDriver = User.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .fullName(fullName.trim())
                .phone(cleanPhone)
                .licensePlate(licensePlate != null ? licensePlate.trim().toUpperCase() : null)
                .role(Role.ROLE_TRUCK_DRIVER)
                .build();

        return userRepository.save(newDriver);
    }
}
