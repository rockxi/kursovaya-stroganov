package com.education.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String username, String password, String email) {
        // Проверка на существование пользователя
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // Создание нового пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Хеширование пароля
        user.setEmail(email);
        user.setRole("USER");

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }
    
    // Получение всех пользователей
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    // Поиск пользователя по ID
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // Обновление пользователя
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    // Удаление пользователя
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    // Проверка существования пользователя по имени
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // Проверка существования пользователя по email
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // Поиск пользователей по имени или email
    public List<User> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    }
}
