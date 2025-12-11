package com.education.backend;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            User user = userService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        boolean authenticated = userService.authenticate(
            request.getUsername(),
            request.getPassword()
        );

        Map<String, Object> response = new HashMap<>();

        if (authenticated) {
            User user = userService.findByUsername(request.getUsername());
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                response
            );
        }
    }
}
