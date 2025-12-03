package com.education.backend;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    private String email; // Используется только для регистрации
}
