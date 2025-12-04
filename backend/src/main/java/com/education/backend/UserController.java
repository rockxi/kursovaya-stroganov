package com.education.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Map;

@Controller
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // API endpoint для получения списка пользователей
    @ResponseBody
    @GetMapping("/api/admin/users")
    public ResponseEntity<List<User>> getUsersApi(@RequestParam(required = false) String query) {
        List<User> users;
        
        if (query != null && !query.isEmpty()) {
            users = userService.searchUsers(query);
        } else {
            users = userService.findAllUsers();
        }
        
        return ResponseEntity.ok(users);
    }

    // Получение списка пользователей с возможностью поиска (REST-контроллер для фронтенда)
    @ResponseBody
    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getUsersRest(@RequestParam(required = false) String query) {
        List<User> users;
        
        if (query != null && !query.isEmpty()) {
            users = userService.searchUsers(query);
        } else {
            users = userService.findAllUsers();
        }
        
        return ResponseEntity.ok(users);
    }
    
    // Получение списка пользователей с возможностью поиска (для прямого просмотра)
    @GetMapping("/view")
    public String getUsers(@RequestParam(required = false) String query, Model model) {
        List<User> users;
        
        if (query != null && !query.isEmpty()) {
            users = userService.searchUsers(query);
        } else {
            users = userService.findAllUsers();
        }
        
        // Добавляем информацию о текущем пользователе в модель
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        User adminUser = userService.findByUsername(currentUser);
        
        model.addAttribute("users", users);
        model.addAttribute("query", query);
        model.addAttribute("username", currentUser);
        model.addAttribute("role", adminUser.getRole());
        model.addAttribute("loggedIn", true);
        
        return "user-management";
    }

    // API endpoint для получения пользователя по ID
    @ResponseBody
    @GetMapping("/api/admin/users/{id}")
    public ResponseEntity<User> getUserByIdApi(@PathVariable Long id) {
        Optional<User> userOptional = userService.findUserById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(userOptional.get());
    }

    // Получение пользователя по ID для редактирования (REST-контроллер для фронтенда)
    @ResponseBody
    @GetMapping("/admin/users/{id}")
    public ResponseEntity<User> getUserByIdRest(@PathVariable Long id) {
        Optional<User> userOptional = userService.findUserById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(userOptional.get());
    }
    
    // Получение пользователя по ID для редактирования (для прямого просмотра)
    @GetMapping("/{id}/view")
    public String getUserById(@PathVariable Long id, Model model) {
        Optional<User> userOptional = userService.findUserById(id);
        
        if (userOptional.isEmpty()) {
            return "redirect:/admin/users?error=userNotFound";
        }
        
        User user = userOptional.get();
        
        // Добавляем информацию о текущем пользователе в модель
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        User adminUser = userService.findByUsername(currentUser);
        
        model.addAttribute("user", user);
        model.addAttribute("username", currentUser);
        model.addAttribute("role", adminUser.getRole());
        model.addAttribute("loggedIn", true);
        
        return "edit-user";
    }

    // API endpoint для обновления пользователя
    @ResponseBody
    @PutMapping("/api/admin/users/{id}")
    public ResponseEntity<?> updateUserApi(@PathVariable Long id, @RequestBody User userForm) {
        Optional<User> userOptional = userService.findUserById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOptional.get();
        
        // Проверка, что имя пользователя и email уникальны (если они изменились)
        if (!user.getUsername().equals(userForm.getUsername()) && 
            userService.existsByUsername(userForm.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "usernameExists"));
        }
        
        if (!user.getEmail().equals(userForm.getEmail()) && 
            userService.existsByEmail(userForm.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "emailExists"));
        }
        
        // Обновляем данные пользователя
        user.setUsername(userForm.getUsername());
        user.setEmail(userForm.getEmail());
        user.setRole(userForm.getRole());
        
        userService.updateUser(user);
        
        return ResponseEntity.ok(Map.of("success", "userUpdated"));
    }

    // Обновление данных пользователя (REST-контроллер для фронтенда)
    @ResponseBody
    @PutMapping("/admin/users/{id}")
    public ResponseEntity<?> updateUserRest(@PathVariable Long id, @RequestBody User userForm) {
        Optional<User> userOptional = userService.findUserById(id);
        
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOptional.get();
        
        // Проверка, что имя пользователя и email уникальны (если они изменились)
        if (!user.getUsername().equals(userForm.getUsername()) && 
            userService.existsByUsername(userForm.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "usernameExists"));
        }
        
        if (!user.getEmail().equals(userForm.getEmail()) && 
            userService.existsByEmail(userForm.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "emailExists"));
        }
        
        // Обновляем данные пользователя
        user.setUsername(userForm.getUsername());
        user.setEmail(userForm.getEmail());
        user.setRole(userForm.getRole());
        
        userService.updateUser(user);
        
        return ResponseEntity.ok(Map.of("success", "userUpdated"));
    }
    
    // Обновление данных пользователя (для прямого просмотра)
    @PostMapping("/{id}/view")
    public String updateUser(@PathVariable Long id, @ModelAttribute User userForm, Model model) {
        Optional<User> userOptional = userService.findUserById(id);
        
        if (userOptional.isEmpty()) {
            return "redirect:/admin/users?error=userNotFound";
        }
        
        User user = userOptional.get();
        
        // Проверка, что имя пользователя и email уникальны (если они изменились)
        if (!user.getUsername().equals(userForm.getUsername()) && 
            userService.existsByUsername(userForm.getUsername())) {
            return "redirect:/admin/users/" + id + "?error=usernameExists";
        }
        
        if (!user.getEmail().equals(userForm.getEmail()) && 
            userService.existsByEmail(userForm.getEmail())) {
            return "redirect:/admin/users/" + id + "?error=emailExists";
        }
        
        // Обновляем данные пользователя
        user.setUsername(userForm.getUsername());
        user.setEmail(userForm.getEmail());
        user.setRole(userForm.getRole());
        
        userService.updateUser(user);
        
        return "redirect:/admin/users?success=userUpdated";
    }

    // API endpoint для удаления пользователя
    @ResponseBody
    @DeleteMapping("/api/admin/users/{id}")
    public ResponseEntity<?> deleteUserApi(@PathVariable Long id) {
        // Проверка, что пользователь не пытается удалить себя
        Optional<User> userOptional = userService.findUserById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        userService.deleteUser(id);
        
        return ResponseEntity.ok(Map.of("success", "userDeleted"));
    }

    // Удаление пользователя (REST-контроллер для фронтенда)
    @ResponseBody
    @DeleteMapping("/admin/users/{id}/delete")
    public ResponseEntity<?> deleteUserRest(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        User adminUser = userService.findByUsername(currentUser);
        
        // Проверка, что пользователь не пытается удалить себя
        Optional<User> userOptional = userService.findUserById(id);
        if (userOptional.isPresent()) {
            User userToDelete = userOptional.get();
            if (userToDelete.getUsername().equals(currentUser)) {
                return ResponseEntity.badRequest().body(Map.of("error", "cannotDeleteSelf"));
            }
        }
        
        userService.deleteUser(id);
        
        return ResponseEntity.ok(Map.of("success", "userDeleted"));
    }
    
    // Удаление пользователя (для прямого просмотра)
    @GetMapping("/{id}/delete/view")
    public String deleteUser(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        User adminUser = userService.findByUsername(currentUser);
        
        // Проверка, что пользователь не пытается удалить себя
        Optional<User> userOptional = userService.findUserById(id);
        if (userOptional.isPresent()) {
            User userToDelete = userOptional.get();
            if (userToDelete.getUsername().equals(currentUser)) {
                return "redirect:/admin/users?error=cannotDeleteSelf";
            }
        }
        
        userService.deleteUser(id);
        
        return "redirect:/admin/users?success=userDeleted";
    }
}