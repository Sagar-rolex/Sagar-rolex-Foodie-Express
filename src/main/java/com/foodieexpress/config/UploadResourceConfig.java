package com.foodieexpress.config;

import com.foodieexpress.service.FoodImageStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private final FoodImageStorageService storage;
    public UploadResourceConfig(FoodImageStorageService storage) { this.storage = storage; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.getUploadDirectory().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/foods/**")
                .addResourceLocations(location);
        String bannerLocation = storage.getRestaurantBannerDirectory().toUri().toString();
        if (!bannerLocation.endsWith("/")) bannerLocation += "/";
        registry.addResourceHandler("/uploads/restaurants/**")
                .addResourceLocations(bannerLocation);
    }
}
