package com.foodieexpress.service;

import com.foodieexpress.model.FoodItem;
import com.foodieexpress.model.Restaurant;
import com.foodieexpress.config.ImageDefaults;
import com.foodieexpress.repository.FoodItemRepository;
import com.foodieexpress.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FoodItemService {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Transactional
    public FoodItem create(Long restaurantId, String name, String description, BigDecimal price, String category, String imagePath) {
        validatePrice(price);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        if (!restaurant.isActive()) throw new RuntimeException("Cannot add food to an inactive restaurant");
        FoodItem item = new FoodItem();
        item.setRestaurant(restaurant);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setCategory(category);
        item.setImagePath(imagePath == null || imagePath.isBlank() ? ImageDefaults.food(item) : imagePath);
        item.setAvailable(true);
        return foodItemRepository.save(item);
    }

    public List<FoodItem> getByRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return foodItemRepository.findByRestaurant(restaurant);
    }

    public List<FoodItem> getAvailableByRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return foodItemRepository.findByRestaurantAndAvailableTrue(restaurant);
    }

    public List<FoodItem> getAllAvailable() {
        return foodItemRepository.findByAvailableTrue();
    }

    public List<FoodItem> getByCategory(String category) {
        return foodItemRepository.findByCategoryIgnoreCase(category);
    }

    public List<FoodItem> searchByName(String keyword) {
        return foodItemRepository.findByNameContainingIgnoreCase(keyword);
    }

    public FoodItem getById(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food item not found"));
    }

    @Transactional
    public FoodItem update(Long id, String name, String description, BigDecimal price, String category, Boolean available, String imagePath) {
        FoodItem item = getById(id);
        if (price != null) validatePrice(price);
        if (name != null) item.setName(name);
        if (description != null) item.setDescription(description);
        if (price != null) item.setPrice(price);
        if (category != null) item.setCategory(category);
        if (imagePath != null) item.setImagePath(imagePath.isBlank() ? ImageDefaults.food(item) : imagePath);
        if (available != null) item.setAvailable(available);
        return foodItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        FoodItem item = getById(id);
        item.setAvailable(false);
        foodItemRepository.save(item);
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.scale() > 2) {
            throw new RuntimeException("Price must be a positive amount with at most two decimal places");
        }
    }
}
