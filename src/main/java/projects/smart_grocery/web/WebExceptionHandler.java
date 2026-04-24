package projects.smart_grocery.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice(annotations = Controller.class)
public class WebExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNotFound(HttpServletRequest request, Model model) {
        model.addAttribute("path", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNoHandlerFound(HttpServletRequest request, Model model) {
        model.addAttribute("path", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleWebException(Exception ex, HttpServletRequest request, Model model) {
        model.addAttribute("message", ex.getMessage() == null ? "Unexpected error" : ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }
}
