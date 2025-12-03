package com.education.frontend;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WebController {

    // URL бэкенда из docker-compose
    @Value("${BACKEND_API_URL:http://localhost:8080/api}")
    private String backendUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();

    private void addUserToModel(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            model.addAttribute("loggedIn", true);
            model.addAttribute("username", username);
            model.addAttribute("email", session.getAttribute("email"));
            model.addAttribute("role", session.getAttribute("role"));
        } else {
            model.addAttribute("loggedIn", false);
        }
    }
    // Главная страница - Список курсов (Таблица - п. 1.3)
    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) String query, HttpSession session) {
        addUserToModel(model, session);
        String url = backendUrl + "/courses";
        if (query != null) {
            url += "?search=" + query;
        }
        
        try {
            // Запрос JSON данных с бэкенда
            Object[] courses = restTemplate.getForObject(url, Object[].class);
            model.addAttribute("courses", courses);
        } catch (Exception e) {
            model.addAttribute("courses", new Object[0]);
            model.addAttribute("error", "Не удалось подключиться к backend");
        }
        return "index"; // Возвращает index.html
    }

    // Страница "Об авторе" (Обязательное требование п. 1.9)
    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        addUserToModel(model, session);
        model.addAttribute("authorName", "Иванов Иван");
        model.addAttribute("group", "ДПИ23-1");
        model.addAttribute("technologies", "Java Spring, PostgreSQL, Docker");
        return "about";
    }
    
    // Вход в систему (п. 1.2)
    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        addUserToModel(model, session);
        return "login";
    }
    
    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        try {
            // Создаем JSON запрос к backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("password", password);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/auth/login",
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                // Сохраняем данные пользователя в сессию
                session.setAttribute("username", responseBody.get("username"));
                session.setAttribute("email", responseBody.get("email"));
                session.setAttribute("role", responseBody.get("role"));
                // Успешная авторизация - перенаправляем на главную
                return "redirect:/?loginSuccess=true";
            } else {
                model.addAttribute("error", "Неверное имя пользователя или пароль");
                return "login";
            }
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (401, 400 и т.д.)
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> errorResponse = mapper.readValue(responseBody, Map.class);
                    String errorMsg = errorResponse.get("message") != null 
                        ? errorResponse.get("message").toString() 
                        : "Неверное имя пользователя или пароль";
                    model.addAttribute("error", errorMsg);
                } else {
                    model.addAttribute("error", "Неверное имя пользователя или пароль");
                }
            } catch (Exception parseException) {
                model.addAttribute("error", "Неверное имя пользователя или пароль");
            }
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка подключения к серверу: " + e.getMessage());
            return "login";
        }
    }
    
    // Регистрация (п. 1.2)
    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        addUserToModel(model, session);
        return "register";
    }
    
    @PostMapping("/register")
    public String registerSubmit(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {
        try {
            // Создаем JSON запрос к backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("email", email);
            request.put("password", password);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/auth/register",
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                model.addAttribute("success", "Регистрация прошла успешно! Теперь вы можете войти.");
                return "register";
            } else {
                String errorMsg = responseBody != null && responseBody.get("message") != null 
                    ? responseBody.get("message").toString() 
                    : "Ошибка регистрации";
                model.addAttribute("error", errorMsg);
                return "register";
            }
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (400, 409 и т.д.)
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> errorResponse = mapper.readValue(responseBody, Map.class);
                    String errorMsg = errorResponse.get("message") != null 
                        ? errorResponse.get("message").toString() 
                        : "Ошибка регистрации";
                    model.addAttribute("error", errorMsg);
                } else {
                    model.addAttribute("error", "Ошибка регистрации");
                }
            } catch (Exception parseException) {
                model.addAttribute("error", "Ошибка регистрации");
            }
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка подключения к серверу: " + e.getMessage());
            return "register";
        }
    }
    
    // Выход из системы
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
