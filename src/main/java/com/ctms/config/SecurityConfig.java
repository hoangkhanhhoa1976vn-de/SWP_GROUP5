package com.ctms.config;

import com.ctms.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF được BẬT (mặc định). Các form Thymeleaf dùng th:action nên tự động chèn token.
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/login").permitAll()
                .requestMatchers("/shipping-lines/**", "/drivers/**").hasAuthority("ROLE_MANAGER")
                .requestMatchers("/portal/shipping-line/**").hasAnyAuthority("ROLE_SHIPPING_LINE", "ROLE_MANAGER")
                .requestMatchers("/portal/driver/**").hasAnyAuthority("ROLE_TRUCK_DRIVER", "ROLE_OPERATOR", "ROLE_MANAGER")
                .requestMatchers("/gate/**", "/yard/**", "/berths/**", "/vessels/**")
                    .hasAnyAuthority("ROLE_OPERATOR", "ROLE_MANAGER")
                .requestMatchers("/containers/**")
                    .hasAnyAuthority("ROLE_OPERATOR", "ROLE_MANAGER", "ROLE_SHIPPING_LINE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
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
