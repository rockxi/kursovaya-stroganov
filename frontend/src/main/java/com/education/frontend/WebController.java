package com.education.frontend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
    public String index(
        Model model,
        @RequestParam(required = false) String query,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String url = backendUrl + "/courses";
        if (query != null) {
            url += "?search=" + query;
        }

        try {
            // Запрос JSON данных с бэкенда
            Object[] courses = restTemplate.getForObject(url, Object[].class);
            model.addAttribute("courses", courses);
            model.addAttribute("query", query);
        } catch (Exception e) {
            model.addAttribute("courses", new Object[0]);
            model.addAttribute("error", "Не удалось подключиться к backend");
        }
        return "index"; // Возвращает index.html
    }

    // Просмотр курса (детальная информация)
    @GetMapping("/courses/{id}")
    public String viewCourse(
        @PathVariable Long id,
        Model model,
        @RequestParam(required = false) String query,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String username = (String) session.getAttribute("username");

        // Проверяем, авторизован ли пользователь. Если нет - перенаправляем на страницу входа
        if (username == null) {
            // Сохраняем ID курса, чтобы после авторизации вернуться к его просмотру
            session.setAttribute("requestedCourseId", id);
            session.setAttribute(
                "error",
                "Для просмотра подробной информации о курсе необходимо войти в систему или зарегистрироваться"
            );
            return "redirect:/login";
        }

        // Получаем все курсы для сайдбара
        String coursesUrl = backendUrl + "/courses";
        if (query != null) {
            coursesUrl += "?search=" + query;
        }

        try {
            Object[] courses = restTemplate.getForObject(
                coursesUrl,
                Object[].class
            );
            model.addAttribute("courses", courses);
            model.addAttribute("query", query);
        } catch (Exception e) {
            model.addAttribute("courses", new Object[0]);
        }

        // Получаем детали выбранного курса
        try {
            Object course = restTemplate.getForObject(
                backendUrl + "/courses/" + id,
                Object.class
            );
            model.addAttribute("selectedCourse", course);
            model.addAttribute("selectedCourseId", id);
        } catch (Exception e) {
            // Проверяем, это ошибка аутентификации или курс не найден
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                // Ошибка аутентификации - перенаправляем на страницу входа
                session.setAttribute("requestedCourseId", id);
                session.setAttribute(
                    "error",
                    "Для просмотра подробной информации о курсе необходимо войти в систему или зарегистрироваться"
                );
                return "redirect:/login";
            } else {
                model.addAttribute("error", "Курс не найден");
            }
        }

        return "index";
    }

    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        addUserToModel(model, session);
        model.addAttribute("authorName", "Строганов Тимофей Александрович");
        model.addAttribute("group", "ПИ23-2В");
        model.addAttribute(
            "technologies",
            "Java Spring как API фреймворк, PostgreSQL как база данных"
        );
        model.addAttribute(
            "deploy_technologies",
            "Docker для развёртывания, Nginx как прокси, Letsencrypt и Certbot для SSL"
        );
        return "about";
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        addUserToModel(model, session);
        model.addAttribute("error", session.getAttribute("error"));
        model.addAttribute("success", session.getAttribute("success"));
        session.removeAttribute("error");
        session.removeAttribute("success");
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(
        @RequestParam String username,
        @RequestParam String password,
        Model model,
        HttpSession session
    ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("password", password);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                request,
                headers
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/auth/login",
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (
                responseBody != null &&
                Boolean.TRUE.equals(responseBody.get("success"))
            ) {
                session.setAttribute("username", responseBody.get("username"));
                session.setAttribute("email", responseBody.get("email"));
                session.setAttribute("role", responseBody.get("role"));

                Long requestedCourseId = (Long) session.getAttribute(
                    "requestedCourseId"
                );
                if (requestedCourseId != null) {
                    session.removeAttribute("requestedCourseId");
                    return "redirect:/courses/" + requestedCourseId;
                }

                return "redirect:/?loginSuccess=true";
            } else {
                session.setAttribute(
                    "error",
                    "Неверное имя пользователя или пароль"
                );
                return "redirect:/login";
            }
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (401, 400 и т.д.)
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> errorResponse = mapper.readValue(
                        responseBody,
                        Map.class
                    );
                    String errorMsg = errorResponse.get("message") != null
                        ? errorResponse.get("message").toString()
                        : "Неверное имя пользователя или пароль";
                    session.setAttribute("error", errorMsg);
                } else {
                    session.setAttribute(
                        "error",
                        "Неверное имя пользователя или пароль"
                    );
                }
            } catch (Exception parseException) {
                session.setAttribute(
                    "error",
                    "Неверное имя пользователя или пароль"
                );
            }
            return "redirect:/login";
        } catch (Exception e) {
            session.setAttribute(
                "error",
                "Ошибка подключения к серверу: " + e.getMessage()
            );
            return "redirect:/login";
        }
    }

    // Регистрация (п. 1.2)
    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        addUserToModel(model, session);
        model.addAttribute("error", session.getAttribute("error"));
        model.addAttribute("success", session.getAttribute("success"));
        // Clear attributes after they have been added to the model
        session.removeAttribute("error");
        session.removeAttribute("success");
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(
        @RequestParam String username,
        @RequestParam String email,
        @RequestParam String password,
        Model model,
        HttpSession session
    ) {
        try {
            // Создаем JSON запрос к backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("email", email);
            request.put("password", password);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                request,
                headers
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/auth/register",
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (
                responseBody != null &&
                Boolean.TRUE.equals(responseBody.get("success"))
            ) {
                // Проверяем, запрашивал ли пользователь определенный курс до регистрации
                Long requestedCourseId = (Long) session.getAttribute(
                    "requestedCourseId"
                );
                if (requestedCourseId != null) {
                    session.setAttribute(
                        "success",
                        "Регистрация прошла успешно! Теперь вы можете войти, чтобы просмотреть курс."
                    );
                } else {
                    session.setAttribute(
                        "success",
                        "Регистрация прошла успешно! Теперь вы можете войти."
                    );
                }
                // Не удаляем requestedCourseId, чтобы пользователь мог попасть на курс после входа
                return "redirect:/login";
            } else {
                String errorMsg = responseBody != null &&
                    responseBody.get("message") != null
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
                    Map<String, Object> errorResponse = mapper.readValue(
                        responseBody,
                        Map.class
                    );
                    String errorMsg = errorResponse.get("message") != null
                        ? errorResponse.get("message").toString()
                        : "Ошибка регистрации";
                    session.setAttribute("error", errorMsg);
                } else {
                    session.setAttribute("error", "Ошибка регистрации");
                }
                return "redirect:/register";
            } catch (Exception parseException) {
                session.setAttribute("error", "Ошибка регистрации");
                return "redirect:/register";
            }
        } catch (Exception e) {
            session.setAttribute(
                "error",
                "Ошибка подключения к серверу: " + e.getMessage()
            );
            return "redirect:/register";
        }
    }

    // Выход из системы
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // Создание курса (только для ADMIN)
    @GetMapping("/courses/create")
    public String createCourse(Model model, HttpSession session) {
        addUserToModel(model, session);
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        return "create-course";
    }

    @PostMapping("/courses/create")
    public String createCourseSubmit(
        @RequestParam String title,
        @RequestParam String description,
        @RequestParam String detailedDescription,
        @RequestParam String category,
        @RequestParam String curriculum,
        Model model,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            // Создаем JSON запрос к backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Получаем ID текущего пользователя для авторства
            Long authorId = null;
            try {
                // В реальном приложении здесь был бы поиск ID пользователя
                // Пока используем значение null для поля authorId
            } catch (Exception e) {
                // ID пользователя не найден, продолжаем без него
            }

            // Преобразуем куррикулум из многострочного текста в формат с разделителем |
            String formattedCurriculum = curriculum
                .trim()
                .replaceAll("\\r?\\n", "|");

            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("detailedDescription", detailedDescription);
            request.put("category", category);
            request.put("curriculum", formattedCurriculum);
            request.put("authorId", authorId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                request,
                headers
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                backendUrl + "/courses",
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return "redirect:/?courseCreated=true";
            } else {
                model.addAttribute("error", "Ошибка при создании курса");
                return "create-course";
            }
        } catch (Exception e) {
            model.addAttribute(
                "error",
                "Ошибка подключения к серверу: " + e.getMessage()
            );
            return "create-course";
        }
    }

    // Управление (только для ADMIN)
    @GetMapping("/admin/users")
    public String userManagement(Model model, HttpSession session) {
        addUserToModel(model, session);
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            // Используем API endpoint для получения пользователей
            String backendUsersUrl = backendUrl + "/admin/users";
            Object[] users = restTemplate.getForObject(
                backendUsersUrl,
                Object[].class
            );
            model.addAttribute("users", users);
        } catch (Exception e) {
            model.addAttribute("users", new Object[0]);
            model.addAttribute(
                "error",
                "Не удалось подключиться к backend: " + e.getMessage()
            );
        }

        return "user-management";
    }

    // Редактирование пользователя (только для ADMIN)
    @GetMapping("/admin/users/{id}")
    public String editUser(
        @PathVariable Long id,
        Model model,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            String backendUserUrl = backendUrl + "/admin/users/" + id;
            Object user = restTemplate.getForObject(
                backendUserUrl,
                Object.class
            );
            model.addAttribute("user", user);
        } catch (Exception e) {
            return "redirect:/admin/users?error=userNotFound";
        }

        return "edit-user";
    }

    // Обновление пользователя (только для ADMIN)
    @PostMapping("/admin/users/{id}")
    public String updateUser(
        @PathVariable Long id,
        @RequestParam String username,
        @RequestParam String email,
        @RequestParam String role,
        Model model,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String userRole = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(userRole)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            // Создаем JSON запрос к backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("username", username);
            request.put("email", email);
            request.put("role", role);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                request,
                headers
            );

            restTemplate.put(
                backendUrl + "/admin/users/" + id,
                entity,
                Map.class
            );

            return "redirect:/admin/users?success=userUpdated";
        } catch (Exception e) {
            model.addAttribute(
                "error",
                "Ошибка при обновлении пользователя: " + e.getMessage()
            );
            return "edit-user";
        }
    }

    // Удаление пользователя (только для ADMIN)
    @GetMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            restTemplate.delete(backendUrl + "/admin/users/" + id);
        } catch (Exception e) {
            return "redirect:/admin/users?error=cannotDeleteUser";
        }

        return "redirect:/admin/users?success=userDeleted";
    }

    // Редактирование курса (только для ADMIN)
    @GetMapping("/courses/{id}/edit")
    public String editCourse(
        @PathVariable Long id,
        Model model,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            // Получаем информацию о курсе
            Object course = restTemplate.getForObject(
                backendUrl + "/courses/" + id,
                Object.class
            );
            model.addAttribute("course", course);
            model.addAttribute("courseId", id);
        } catch (Exception e) {
            return "redirect:/?error=courseNotFound";
        }

        // Передаем сообщение об ошибке из сессии, если есть
        model.addAttribute("error", session.getAttribute("error"));
        session.removeAttribute("error");

        return "edit-course";
    }

    // Обновление курса (только для ADMIN)
    @PostMapping("/courses/{id}/edit")
    public String updateCourse(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam String description,
        @RequestParam String detailedDescription,
        @RequestParam String category,
        @RequestParam String curriculum,
        Model model,
        HttpSession session
    ) {
        addUserToModel(model, session);
        String role = (String) session.getAttribute("role");

        // Проверка прав доступа
        if (!"ADMIN".equals(role)) {
            return "redirect:/?error=accessDenied";
        }

        try {
            // Создаем JSON запрос к backend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Преобразуем куррикулум из многострочного текста в формат с разделителем |
            String formattedCurriculum = curriculum
                .trim()
                .replaceAll("\\r?\\n", "|");

            Map<String, Object> request = new HashMap<>();
            request.put("title", title);
            request.put("description", description);
            request.put("detailedDescription", detailedDescription);
            request.put("category", category);
            request.put("curriculum", formattedCurriculum);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                request,
                headers
            );

            restTemplate.put(backendUrl + "/courses/" + id, entity, Map.class);

            return "redirect:/courses/" + id + "?courseUpdated=true";
        } catch (Exception e) {
            session.setAttribute(
                "error",
                "Ошибка при обновлении курса: " + e.getMessage()
            );
            return "redirect:/courses/" + id + "/edit";
        }
    }
}
