package io.skillswap.springweb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Lets a separately-hosted SkillSwap frontend call this backend's REST API.
// Auth here is session-cookie based (same as the Thymeleaf pages), so if you
// call the API from a different origin, the frontend must send requests with
// credentials included (fetch(url, { credentials: "include" })) for the
// session cookie to be sent/received.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
