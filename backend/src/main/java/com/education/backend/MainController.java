package com.education.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(@RequestParam(required = false) String query, Model model, HttpServletRequest request) {
        // Получаем информацию о текущем пользователе
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && !auth.getName().equals("anonymousUser");
        
        if (isAuthenticated) {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            model.addAttribute("loggedIn", true);
            model.addAttribute("username", username);
            model.addAttribute("email", user != null ? user.getEmail() : "");
            model.addAttribute("role", user != null ? user.getRole() : "USER");
        } else {
            model.addAttribute("loggedIn", false);
        }
        
        // Получаем курсы с учетом поискового запроса
        if (query != null && !query.isEmpty()) {
            model.addAttribute("courses", courseRepository.findByTitleContainingIgnoreCase(query));
            model.addAttribute("query", query);
        } else {
            model.addAttribute("courses", courseRepository.findAll());
        }
        
        // Добавляем параметры для отображения сообщений об успешных действиях
        model.addAttribute("param", request.getParameterMap());
        
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        // Получаем информацию о текущем пользователе
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && !auth.getName().equals("anonymousUser");
        
        if (isAuthenticated) {
            return "redirect:/";
        }
        
        return "register";
    }
    
    @PostMapping("/register")
    public String processRegistration(@RequestParam String username, 
                                     @RequestParam String email, 
                                     @RequestParam String password, 
                                     Model model) {
        try {
            User user = userService.registerUser(username, password, email);
            model.addAttribute("success", "Регистрация прошла успешно! Теперь вы можете войти в систему.");
            return "login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
    
    @PostMapping("/login")
    public String processLogin(@RequestParam String username, 
                              @RequestParam String password,
                              HttpServletRequest request,
                              Model model) {
        boolean authenticated = userService.authenticate(username, password);
        
        if (authenticated) {
            // Устанавливаем сессию
            request.getSession().setAttribute("user", username);
            return "redirect:/?loginSuccess=true";
        } else {
            model.addAttribute("error", "Неверное имя пользователя или пароль.");
            return "login";
        }
    }

    @GetMapping("/about")
    public String about(Model model) {
        // Получаем информацию о текущем пользователе
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && !auth.getName().equals("anonymousUser");
        
        if (isAuthenticated) {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            model.addAttribute("loggedIn", true);
            model.addAttribute("username", username);
            model.addAttribute("email", user != null ? user.getEmail() : "");
            model.addAttribute("role", user != null ? user.getRole() : "USER");
        } else {
            model.addAttribute("loggedIn", false);
        }
        
        model.addAttribute("authorName", "Строганов Т.А.");
        model.addAttribute("group", "ИУ5-53Б");
        model.addAttribute("technologies", "Spring Boot, Thymeleaf, PostgreSQL");

        return "about";
    }

    @GetMapping("/courses/create")
    public String createCourse(Model model) {
        // Получаем информацию о текущем пользователе
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && !auth.getName().equals("anonymousUser");
        
        if (isAuthenticated) {
            String username = auth.getName();
            User user = userService.findByUsername(username);
            
            model.addAttribute("loggedIn", true);
            model.addAttribute("username", username);
            model.addAttribute("email", user != null ? user.getEmail() : "");
            model.addAttribute("role", user != null ? user.getRole() : "USER");
            
            // Проверяем, что пользователь с ролью ADMIN
            if (!user.getRole().equals("ADMIN")) {
                return "redirect:/?error=accessDenied";
            }
        } else {
            return "redirect:/login";
        }
        
        return "create-course";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        // Инвалидация сессии
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        
        return "redirect:/login";
    }
}