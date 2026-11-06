package com.foodieexpress.service;

import com.foodieexpress.model.Restaurant;
import com.foodieexpress.config.ImageDefaults;
import com.foodieexpress.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    public Restaurant create(String name, String address, String cuisineType, String description, Double rating, Integer deliveryTime, String imagePath, String bannerImagePath) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setAddress(address);
        restaurant.setCuisineType(cuisineType);
        restaurant.setDescription(description);
        restaurant.setRating(rating);
        restaurant.setDeliveryTime(deliveryTime);
        restaurant.setImagePath(imagePath == null || imagePath.isBlank() ? ImageDefaults.restaurant(restaurant) : imagePath);
        restaurant.setBannerImagePath(bannerImagePath == null || bannerImagePath.isBlank() ? null : bannerImagePath);
        restaurant.setActive(true);
        return restaurantRepository.save(restaurant);
    }

    public List<Restaurant> getAllActive() {
        return restaurantRepository.findByActiveTrue();
    }

    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    public Restaurant update(Long id, String name, String address, String cuisineType, String description, Double rating, Integer deliveryTime, Boolean active, String imagePath, String bannerImagePath) {
        Restaurant restaurant = getById(id);
        if (name != null) restaurant.setName(name);
        if (address != null) restaurant.setAddress(address);
        if (cuisineType != null) restaurant.setCuisineType(cuisineType);
        if (description != null) restaurant.setDescription(description);
        if (rating != null) restaurant.setRating(rating);
        if (deliveryTime != null) restaurant.setDeliveryTime(deliveryTime);
        if (imagePath != null) restaurant.setImagePath(imagePath.isBlank() ? ImageDefaults.restaurant(restaurant) : imagePath);
        if (bannerImagePath != null) restaurant.setBannerImagePath(bannerImagePath.isBlank() ? null : bannerImagePath);
        if (active != null) restaurant.setActive(active);
        return restaurantRepository.save(restaurant);
    }

    public void delete(Long id) {
        Restaurant restaurant = getById(id);
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
    }
}
