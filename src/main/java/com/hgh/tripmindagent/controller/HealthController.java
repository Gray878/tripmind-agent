package com.hgh.tripmindagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String healthCheck() {
        return "ok";
    }

    @GetMapping("/detail")
    public Map<String, Object> healthDetail() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("processors", runtime.availableProcessors());
        system.put("totalMemory", runtime.totalMemory());
        system.put("freeMemory", runtime.freeMemory());

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", "UP");
        components.put("vectorStore", "UP");
        components.put("aiModel", "UP");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", "UP");
        detail.put("version", "1.0.0");
        detail.put("timestamp", LocalDateTime.now().toString());
        detail.put("system", system);
        detail.put("components", components);
        return detail;
    }
}
