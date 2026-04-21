package com.backend.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant timestamp;
    
    private HttpStatus status;
    
    private String message;
    
    private String path;
    
    private List<String> errors;
    
    public static ApiError of(HttpStatus status, String message, String path) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .path(path)
                .build();
    }
    
    public static ApiError validation(HttpStatus status, String message, String path, List<String> errors) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }
}