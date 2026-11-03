package com.ctms.config;

import com.ctms.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(UserService userService,
                          CustomAuthenticationSuccessHandler successHandler,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.userService = userService;
        this.successHandler = successHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new MultiRoleSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
            .authorizeHttpRequests(auth -> auth
                // Tài nguyên công khai & Đăng nhập
                .requestMatchers("/css/**", "/js/**", "/images/**", "/h2-console/**", "/login", "/quick-login", "/register", "/role-logout").permitAll()

                // Luồng 1: Manager (Quản lý Cảng)
                .requestMatchers("/manager/**").hasAuthority("ROLE_MANAGER")
                .requestMatchers("/shipping-lines/**", "/drivers/**").hasAuthority("ROLE_MANAGER")

                // Luồng 2: Operator (Điều hành Hiện trường)
                .requestMatchers("/operator/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_MANAGER")
                .requestMatchers("/gate/**", "/yard/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_MANAGER")
                .requestMatchers("/berths/**", "/vessels/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_MANAGER")
                .requestMatchers("/containers/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_MANAGER", "ROLE_SHIPPING_LINE")

                // Luồng 3: Shipping Line (Cổng Hãng Tàu)
                .requestMatchers("/portal/shipping-line/**").hasAnyAuthority("ROLE_SHIPPING_LINE", "ROLE_MANAGER")

                // Luồng 4: Truck Driver (Cổng Tài Xế)
                .requestMatchers("/portal/driver/**").hasAnyAuthority("ROLE_TRUCK_DRIVER", "ROLE_OPERATOR", "ROLE_MANAGER")

                // Các trang chung còn lại
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
