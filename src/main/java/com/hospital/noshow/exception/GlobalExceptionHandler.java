package com.hospital.noshow.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 — ResourceNotFoundException ──────────────────────────────────────
    // Thrown by: PatientServiceImpl.findById, AppointmentServiceImpl.findById,
    // DoctorServiceImpl.findByUsername, HospitalScheduler Job 3 admin-lookup.
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    // ── 403 — AccessDeniedException ───────────────────────────────────────────
    // Thrown by Spring Security when a role-gated route is hit by the wrong role.
    // Confirmed working in item 21's Swagger Bearer-token verification.
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request,
                                     Model model) {
        log.warn("Access denied to [{}]: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message",
                "You don't have permission to access this page. " +
                        "Please log in with an account that has the required role.");
        return "error";
    }

    // ── 500 — generic fallback ────────────────────────────────────────────────
    // Catches anything not handled above — Flask-down exceptions that escape
    // the scheduler's per-record try/catch, unexpected service failures, etc.
    // Sanitized: stack trace goes to log only, generic message goes to view.
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex,
                                HttpServletRequest request,
                                Model model) {
        log.error("Unexpected error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message",
                "Something went wrong on our end. Please try again or contact support.");
        return "error";
    }
}