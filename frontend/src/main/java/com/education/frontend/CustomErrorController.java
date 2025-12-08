package com.education.frontend;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(
        HttpServletRequest request,
        Model model,
        HttpSession session
    ) {
        // Add user info to model
        String username = (String) session.getAttribute("username");
        if (username != null) {
            model.addAttribute("loggedIn", true);
            model.addAttribute("username", username);
            model.addAttribute("email", session.getAttribute("email"));
            model.addAttribute("role", session.getAttribute("role"));
        } else {
            model.addAttribute("loggedIn", false);
        }

        // Get error status code
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Integer statusCode = status != null ? Integer.valueOf(status.toString()) : 500;

        // Get error details
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Exception exception = (Exception) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Set error attributes based on status code
        model.addAttribute("code", statusCode);

        if (statusCode == 404) {
            model.addAttribute("title", "Страница не найдена");
            model.addAttribute(
                "message",
                "Запрошенная страница не существует. Проверьте правильность URL."
            );
        } else if (statusCode == 403) {
            model.addAttribute("title", "Доступ запрещен");
            model.addAttribute(
                "message",
                "У вас нет прав для доступа к этому ресурсу."
            );
        } else if (statusCode == 500) {
            model.addAttribute("title", "Внутренняя ошибка сервера");
            model.addAttribute(
                "message",
                "Произошла ошибка при обработке вашего запроса."
            );
        } else {
            model.addAttribute("title", "Ошибка " + statusCode);
            model.addAttribute(
                "message",
                errorMessage != null
                    ? errorMessage
                    : "Произошла непредвиденная ошибка."
            );
        }

        // Add technical details if available
        if (requestUri != null) {
            model.addAttribute("url", requestUri);
        }
        if (exception != null) {
            model.addAttribute("exception", exception.getClass().getSimpleName());
        }

        return "error";
    }
}
