package com.backend.demo.controller;

import com.backend.demo.dto.ChangePasswordRequest;
import com.backend.demo.dto.LoginRequest;
import com.backend.demo.dto.RegisterRequest;
import com.backend.demo.dto.ResponseData;
import com.backend.demo.security.jwt.JwtUtils;
import com.backend.demo.security.user.UserDetailService;
import com.backend.demo.security.utils.CookieUtils;
import com.backend.demo.service.auth.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/auth")
public class AuthController {

    private final IAuthService authService;

    @Value("${auth.token.refreshExpirationInMils}")
    private Long refreshTokenExpirationTime;

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseData> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.authenticateUser(request, response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ResponseData> refreshAccessToken(HttpServletRequest request) {
        return authService.refreshAccessToken(request);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }

    @PostMapping("/change-password")
    public ResponseEntity<ResponseData> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return authService.changePassword(request, authentication, httpRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        return authService.logout(response, request);
    }
}