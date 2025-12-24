package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // HTML Endpoint for Selenium Testing
    @GetMapping("/")
    public String home() {
        return "<html><head><title>Home</title></head><body>" +
               "<h1 id='welcome-message'>Welcome to the JDK 21 App</h1>" +
               "<p>Version: 1.0.0</p>" +
               "</body></html>";
    }

    // JSON Endpoint for API Testing
    @GetMapping("/api/greet")
    public GreetResponse greet(@RequestParam(value = "name", defaultValue = "World") String name) {
        return new GreetResponse("Hello, " + name + "!");
    }

    // Simple DTO
    record GreetResponse(String message) {}
}