package com.education.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Отключаем CSRF для REST API
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/api/auth/**")
                    .permitAll() // Разрешаем доступ к регистрации и логину
                    .requestMatchers("/api/courses/**")
                    .permitAll() // Разрешаем доступ ко всем данным о курсах (фронтенд сам ограничивает доступ к деталям)
                    .requestMatchers("/api/admin/users/**")
                    .permitAll() // Разрешаем доступ к API управления пользователями
                    .requestMatchers("/admin/users")
                    .hasRole("ADMIN") // Только для администраторов
                    .requestMatchers("/admin/users/**")
                    .hasRole("ADMIN") // Только для администраторов
                    .requestMatchers(
                        "/login",
                        "/register",
                        "/about",
                        "/css/**",
                        "/js/**"
                    )
                    .permitAll() // Разрешаем доступ к базовым страницам
                    .requestMatchers("/", "/courses/create")
                    .authenticated() // Требует аутентификации
                    .requestMatchers("/logout")
                    .authenticated() // Для выхода нужна аутентификация
                    .anyRequest()
                    .authenticated()
            )
            .httpBasic(basic -> {}); // Используем HTTP Basic Authentication

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
