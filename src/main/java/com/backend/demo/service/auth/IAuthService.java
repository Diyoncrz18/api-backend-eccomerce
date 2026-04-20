package com.backend.demo.service.auth;

import com.backend.demo.dto.ChangePasswordRequest;
import com.backend.demo.dto.LoginRequest;
import com.backend.demo.dto.RegisterRequest;
import com.backend.demo.dto.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface IAuthService {
    ResponseEntity<ResponseData> authenticateUser(LoginRequest request, HttpServletResponse response);
    ResponseEntity<ResponseData> refreshAccessToken(HttpServletRequest request);
    ResponseEntity<?> getCurrentUser(Authentication authentication);
    ResponseEntity<ResponseData> changePassword(ChangePasswordRequest request,
                                                Authentication authentication,
                                                HttpServletRequest httpRequest);
    ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest httpServletRequest);

    ResponseEntity<ResponseData> register(RegisterRequest request);
}
