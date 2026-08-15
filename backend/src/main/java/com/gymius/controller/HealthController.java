package com.gymius.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Integer databaseCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Map.of(
                "status", "ok",
                "database", databaseCheck != null && databaseCheck == 1 ? "ready" : "unavailable"
        );
    }
}
