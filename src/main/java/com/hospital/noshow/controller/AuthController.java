package com.hospital.noshow.controller;

import com.hospital.noshow.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/auth/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // Role comes off the authenticated GrantedAuthority (e.g. "ROLE_ADMIN")
            // rather than a separate UserRepository lookup — UserDetailsServiceImpl
            // already attached it during authentication.
            String authority = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElseThrow(() -> new BadCredentialsException("No role assigned"));
            String role = authority.replace("ROLE_", "");

            String token = jwtUtil.generateToken(username, role);

            HttpSession session = request.getSession(true);
            session.setAttribute("JWT_TOKEN", token);

            return switch (role) {
                case "ADMIN" -> "redirect:/admin/dashboard";
                case "RECEPTIONIST" -> "redirect:/receptionist/dashboard";
                case "DOCTOR" -> "redirect:/doctor/dashboard";
                default -> "redirect:/login?error=true";
            };

        } catch (AuthenticationException e) {
            // Only genuine login failures (bad credentials, unknown user) land here.
            return "redirect:/login?error=true";
        }
    }

    @PostMapping("/auth/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message",
                "You don't have permission to access this page. " +
                        "Please log in with an account that has the required role.");
        return "error";
    }
}