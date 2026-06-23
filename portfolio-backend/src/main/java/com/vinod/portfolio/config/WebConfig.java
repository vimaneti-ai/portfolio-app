package com.vinod.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the Angular app (running on a different origin during development,
 * and on Vercel in production) to call this API.
 *
 * Update the allowed origins with your real deployed frontend URL.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:4200",
                        "http://vinod-portfolio-2026.s3-website.us-east-2.amazonaws.com",
                        "http://3.150.38.140:8080"
                )
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
