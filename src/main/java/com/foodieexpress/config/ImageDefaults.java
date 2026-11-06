package com.foodieexpress.config;

import com.foodieexpress.model.FoodItem;
import com.foodieexpress.model.Restaurant;

import java.util.Locale;

/** Stable local image paths for demo records; values map to frontend/public/images. */
public final class ImageDefaults {
    private ImageDefaults() {}

    public static String restaurant(Restaurant restaurant) {
        String name = lower(restaurant.getName());
        String cuisine = lower(restaurant.getCuisineType());
        if (name.contains("dakshin") || cuisine.contains("south indian")) return "/images/dosa.jpg";
        if (name.contains("burger") || cuisine.contains("burger")) return "/images/burger.jpg";
        if (name.contains("dragon") || cuisine.contains("chinese")) return "/images/noodles.jpg";
        if (name.contains("taco") || cuisine.contains("mexican") || cuisine.contains("wrap")) return "/images/taco.jpg";
        if (name.contains("healthy") || cuisine.contains("healthy")) return "/images/salad.jpg";
        if (cuisine.contains("japanese") || name.contains("sushi")) return "/images/sushi.jpg";
        if (cuisine.contains("indian") || name.contains("spice") || name.contains("bombay")) return "/images/indian-thali.jpg";
        if (cuisine.contains("fusion")) return "/images/shared-meal.jpg";
        return "/images/restaurant-interior.jpg";
    }

    public static String food(FoodItem item) {
        String name = lower(item.getName());
        String category = lower(item.getCategory());
        String cuisine = item.getRestaurant() == null ? "" : lower(item.getRestaurant().getCuisineType());
        String searchable = name + " " + category;

        if (contains(searchable, "burger", "patty")) return "/images/burger.jpg";
        if (contains(searchable, "pizza")) return "/images/pizza.jpg";
        if (contains(searchable, "sushi", "california roll")) return "/images/sushi.jpg";
        if (contains(searchable, "ramen", "noodle", "momo", "dumpling", "soup", "fried rice", "manchurian", "spring roll", "chilli", "schezwan")) return "/images/noodles.jpg";
        if (contains(searchable, "dosa", "idli", "uttapam", "pongal", "appam", "sambar", "medu vada")) return "/images/dosa.jpg";
        if (contains(searchable, "taco", "burrito", "quesadilla", "frankie", "kathi roll", "shawarma", "hummus", "falafel", "wrap", "pita")) return "/images/taco.jpg";
        if (contains(searchable, "salad", "bowl", "sprouts", "fruit", "oats", "grilled chicken", "grilled paneer")) return "/images/salad.jpg";
        if (contains(searchable, "gulab jamun", "rasmalai", "halwa", "kheer", "jalebi", "rasgulla", "dessert", "lassi", "milkshake", "shake")) return "/images/dessert.jpg";
        if (contains(searchable, "coffee", "chai", "tea", "juice", "soda", "buttermilk", "coconut water", "beverage")) return "/images/coffee.jpg";
        if (contains(searchable, "chaat", "pav bhaji", "vada pav", "pani puri", "sev puri", "bhel puri", "samosa", "kachori", "dhokla", "khandvi", "poha")) return "/images/indian-thali.jpg";
        if (cuisine.contains("south indian")) return "/images/dosa.jpg";
        if (cuisine.contains("chinese")) return "/images/noodles.jpg";
        if (cuisine.contains("japanese")) return "/images/sushi.jpg";
        if (cuisine.contains("mexican")) return "/images/taco.jpg";
        if (cuisine.contains("healthy")) return "/images/salad.jpg";
        if (cuisine.contains("indian")) return "/images/curry.jpg";
        return "/images/shared-meal.jpg";
    }

    private static boolean contains(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
