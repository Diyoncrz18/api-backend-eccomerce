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
@RequestMapping("${api.prefix}/admin/rewards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRewardsController {

    private final AppSettingService appSettingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRewardsConfig() {
        log.info("Admin get rewards config");

        return ResponseEntity.ok(appSettingService.getGroup("rewards"));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateRewardsConfig(@RequestBody Map<String, Object> config) {
        log.info("Admin update rewards config");

        Map<String, Object> updatedConfig = appSettingService.updateGroup("rewards", config);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rewards config updated",
                "settings", updatedConfig
        ));
    }
}
