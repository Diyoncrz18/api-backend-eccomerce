package com.backend.demo.controller;

import com.backend.demo.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AppSettingService appSettingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        log.info("Admin get settings");

        return ResponseEntity.ok(appSettingService.getGroup("store"));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        log.info("Admin update settings");

        Map<String, Object> updatedSettings = appSettingService.updateGroup("store", settings);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Settings updated successfully",
                "settings", updatedSettings
        ));
    }
}
