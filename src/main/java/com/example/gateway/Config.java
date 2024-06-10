package com.example.gateway;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class Config {
    private List<String> configuredApiRouters = List.of(
            "http://localhost:8081/get/api-router",
            "http://localhost:8082/get/api-router"
    );

    private String coreUri = "http://localhost:8088/get/core";
}
