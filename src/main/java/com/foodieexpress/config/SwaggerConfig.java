package com.foodieexpress.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "X-Session-Token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT obtained from login"
)
@OpenAPIDefinition(info = @Info(
        title = "Foodie Express API",
        description = "Online Food Ordering API - Customers browse restaurants, add to cart, place orders, and view bills. Admins manage restaurants and food items.",
        version = "v1.0",
        contact = @Contact(name = "Foodie Express", email = "foodie-express@example.com")
))
public class SwaggerConfig {
}