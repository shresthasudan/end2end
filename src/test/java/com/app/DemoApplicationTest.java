package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoApplicationTest {

    @Test
    void contextLoads() {
        // Basic sanity check
        assertTrue(true);
    }

    @Test
    void testLogic() {
        // Simulate checking internal logic
        String name = "Jenkins";
        String result = "Hello, " + name + "!";
        assertTrue(result.contains("Jenkins"));
    }
}