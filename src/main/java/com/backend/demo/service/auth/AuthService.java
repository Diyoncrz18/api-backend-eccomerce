package com.backend.demo.service.auth;

import com.backend.demo.dto.ChangePasswordRequest;
import com.backend.demo.dto.LoginRequest;
import com.backend.demo.dto.RegisterRequest;
import com.backend.demo.dto.ResponseData;
import com.backend.demo.model.Role;
import com.backend.demo.model.User;
import com.backend.demo.repository.UserRepository;
import com.backend.demo.security.jwt.JwtUtils;
import com.backend.demo.security.user.UserDetailService;
import com.backend.demo.security.utils.CookieUtils;
import com.backend.demo.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;
    private final UserDetailService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    @Value("${auth.token.refreshExpirationInMils}")
    private Long refreshTokenExpirationTime;

    @Override
    @Transactional
    public ResponseEntity<ResponseData> register(RegisterRequest request) {
        ResponseData response = new ResponseData();

        try {
            log.info("Register attempt for email={}", request.getEmail());

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                response.setStatus(false);
                response.getMessage().add("Password dan confirm password tidak sama");
                return ResponseEntity.badRequest().body(response);
            }

            User existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser != null) {
                response.setStatus(false);
                response.getMessage().add("Email sudah terdaftar");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            String encodedPassword = passwordEncoder.encode(request.getPassword());

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(encodedPassword);

            Role roleUser = new Role("ROLE_USER");
            user.getRoles().add(roleUser);

            userRepository.save(user);

            log.info("User registered successfully: {}", request.getEmail());

            response.setStatus(true);
            response.getMessage().add("Registrasi berhasil");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during registration", e);

            response.setStatus(false);
            response.getMessage().add("Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<ResponseData> authenticateUser(LoginRequest request, HttpServletResponse response) {
        ResponseData responseData = new ResponseData();

        try {
            log.info("Login attempt for email={}", request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            log.info("User authenticated successfully: {}", request.getEmail());

            User user = userRepository.findByEmail(request.getEmail());
            if(Objects.isNull(user)){
                throw new UsernameNotFoundException("User not found");
            }


            userRepository.save(user);

            String accessToken = jwtUtils.generateAccessTokenForUser(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(request.getEmail());

            cookieUtils.addRefreshTokenCookie(response, refreshToken, refreshTokenExpirationTime);
            log.debug("Refresh token set in cookie for user={}", request.getEmail());

            Map<String, String> token = new HashMap<>();
            token.put("accessToken", accessToken);

            responseData.setStatus(true);
            responseData.getMessage().add("Login successful");
            responseData.setPayload(token);

            return ResponseEntity.ok(responseData);

        } catch (BadCredentialsException e) {
            log.warn("Invalid login attempt for email={}", request.getEmail());
            responseData.setStatus(false);
            responseData.getMessage().add("Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData);

        } catch (UsernameNotFoundException e) {
            log.warn("Login failed: user not found email={}", request.getEmail());

            responseData.setStatus(false);
            responseData.getMessage().add("User not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData);

        } catch (InternalAuthenticationServiceException e) {
            log.error("Auth service exception during login for email={}", request.getEmail(), e);
            responseData.setStatus(false);
            responseData.getMessage().add("Authentication service error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseData);

        } catch (Exception e) {
            log.error("Unexpected error during authentication for email={}", request.getEmail(), e);
            responseData.setStatus(false);
            responseData.getMessage().add("Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseData);
        }
    }

    @Override
    public ResponseEntity<ResponseData> refreshAccessToken(HttpServletRequest request) {
        ResponseData response = new ResponseData();
        try {
            log.info("Refresh token attempt");
            cookieUtils.logCookies(request);

            String refreshToken = cookieUtils.getRefreshTokenFromCookies(request);

            if (refreshToken != null) {
                boolean isValid = jwtUtils.validateToken(refreshToken);
                log.debug("Refresh token validation result={}", isValid);

                if (isValid) {
                    String usernameFromToken = jwtUtils.getUsernameFromToken(refreshToken);
                    log.info("Refresh token valid for user={}", usernameFromToken);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(usernameFromToken);

                    String newAccessToken = jwtUtils.generateAccessTokenForUser(
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            )
                    );

                    if (newAccessToken != null) {
                        Map<String, String> token = new HashMap<>();
                        token.put("accessToken", newAccessToken);

                        response.setStatus(true);
                        response.getMessage().add("Success generate refresh token");
                        response.setPayload(token);

                        log.info("New access token generated for user={}", usernameFromToken);
                        return ResponseEntity.ok(response);
                    } else {
                        log.error("Failed to generate new access token");
                        response.setStatus(false);
                        response.getMessage().add("Error generate token");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during refresh token process", e);

            response.setStatus(false);
            response.getMessage().add("error : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        log.warn("Invalid or expired refresh token");

        response.setStatus(false);
        response.getMessage().add("Invalid or expired refresh token");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @Override
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized getCurrentUser attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not authenticated");
        }

        String email = authentication.getName();
        log.info("Fetching current user info for email={}", email);

        try {
            Map<String, Object> userInfo = Map.of(
                    "email", email,
                    "authorities", authentication.getAuthorities()
            );

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Error fetching current user info for email={}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching user information");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseData> changePassword(ChangePasswordRequest request,
                                                       Authentication authentication,
                                                       HttpServletRequest httpRequest) {
        ResponseData responseData = new ResponseData();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                responseData.setStatus(false);
                responseData.getMessage().add("User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData);
            }

            String email = authentication.getName();
            log.info("Change password attempt for user={}", email);

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password mismatch for user={}", email);
                responseData.setStatus(false);
                responseData.getMessage().add("New password and confirm password do not match");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseData);
            }

            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                log.warn("New password same as current for user={}", email);

                responseData.setStatus(false);
                responseData.getMessage().add("New password cannot be the same as current password");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseData);
            }

            User user = userRepository.findByEmail(email);
            if (user == null) {
                log.error("User not found in database: {}", email);

                responseData.setStatus(false);
                responseData.getMessage().add("User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseData);
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Invalid current password for user={}", email);

                responseData.setStatus(false);
                responseData.getMessage().add("Current password is incorrect");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData);
            }

            String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPassword(encodedNewPassword);

            userRepository.save(user);

            log.info("Password changed successfully for user={}", email);

            responseData.setStatus(true);
            responseData.getMessage().add("Password changed successfully");

            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            log.error("Error during password change for user={}",
                    authentication != null ? authentication.getName() : "unknown", e);

            responseData.setStatus(false);
            responseData.getMessage().add("Error changing password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseData);
        }
    }

    @Override
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String userEmail = auth.getName();

                cookieUtils.clearRefreshTokenCookie(response);

                return ResponseEntity.ok(Map.of(
                        "message", "Logged out successfully",
                        "user", userEmail
                ));
            } else {
                cookieUtils.clearRefreshTokenCookie(response);

                log.info("Logout called without authenticated user");
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }

        } catch (Exception e) {
            log.error("Error during logout", e);
            try {
                cookieUtils.clearRefreshTokenCookie(response);
            } catch (Exception cookieError) {
                log.error("Failed to clear cookies during error handling", cookieError);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Logout error: " + e.getMessage()));
        }
    }
}