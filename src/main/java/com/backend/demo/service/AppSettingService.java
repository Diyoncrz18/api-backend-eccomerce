package com.backend.demo.service;

import com.backend.demo.model.AppSetting;
import com.backend.demo.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository appSettingRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getGroup(String group) {
        Map<String, Object> settings = new LinkedHashMap<>();
        appSettingRepository.findByGroupOrderByKeyAsc(group)
                .forEach(setting -> settings.put(setting.getKey(), parseValue(setting)));
        return settings;
    }

    @Transactional
    public Map<String, Object> updateGroup(String group, Map<String, Object> values) {
        values.forEach((key, value) -> {
            AppSetting setting = appSettingRepository.findById(key)
                    .orElseGet(() -> AppSetting.builder()
                            .key(key)
                            .group(group)
                            .build());
            setting.setGroup(group);
            setting.setValue(value != null ? String.valueOf(value) : null);
            setting.setValueType(typeOf(value));
            appSettingRepository.save(setting);
        });

        return getGroup(group);
    }

    @Transactional
    public void putIfAbsent(String group, String key, Object value) {
        if (appSettingRepository.existsById(key)) {
            return;
        }

        appSettingRepository.save(AppSetting.builder()
                .key(key)
                .group(group)
                .value(value != null ? String.valueOf(value) : null)
                .valueType(typeOf(value))
                .build());
    }

    private Object parseValue(AppSetting setting) {
        if (setting.getValue() == null) {
            return null;
        }

        return switch (setting.getValueType()) {
            case "integer" -> Integer.parseInt(setting.getValue());
            case "decimal" -> new BigDecimal(setting.getValue());
            case "boolean" -> Boolean.parseBoolean(setting.getValue());
            default -> setting.getValue();
        };
    }

    private String typeOf(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return "integer";
        }
        if (value instanceof Number) {
            return "decimal";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return "string";
    }
}
