package com.foodieexpress.config;

import com.foodieexpress.model.FoodItem;
import com.foodieexpress.model.Restaurant;

import java.io.BufferedReader;
import java.util.Locale;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Stable local image paths for demo records; values map to frontend/public/images. */
public final class ImageDefaults {
    private static final Map<String, String> FOOD_BANNER_CATALOG = loadFoodBannerCatalog();

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
        String restaurantName = item.getRestaurant() == null ? "" : lower(item.getRestaurant().getName());
        String searchable = name + " " + category;

        if (item.getRestaurant() != null) {
            String catalogImage = FOOD_BANNER_CATALOG.get(item.getRestaurant().getName() + "||" + item.getName());
            if (catalogImage != null) return catalogImage;
        }

        if (restaurantName.contains("dakshin rasoi")) {
            if (name.equals("filter coffee")) return "/images/food-banners/dakshin-rasoi-filter-coffee.jpg";
            if (name.equals("masala dosa")) return "/images/food-banners/dakshin-rasoi-masala-dosa.jpg";
            if (name.equals("mysore masala dosa")) return "/images/food-banners/dakshin-rasoi-mysore-masala-dosa.jpg";
            if (name.equals("plain dosa")) return "/images/food-banners/dakshin-rasoi-plain-dosa.jpg";
            if (name.equals("rava dosa")) return "/images/food-banners/dakshin-rasoi-rava-dosa.jpg";
            if (name.equals("set dosa")) return "/images/food-banners/dakshin-rasoi-set-dosa.jpg";
            if (name.equals("onion uttapam")) return "/images/food-banners/dakshin-rasoi-onion-uttapam.jpg";
            if (name.equals("idli sambar (3 pcs)")) return "/images/food-banners/dakshin-rasoi-idli-sambar.jpg";
            if (name.equals("medu vada (2 pcs)")) return "/images/food-banners/dakshin-rasoi-medu-vada.jpg";
            if (name.equals("idli vada combo")) return "/images/food-banners/dakshin-rasoi-idli-vada-combo.jpg";
            if (name.equals("podi idli")) return "/images/food-banners/dakshin-rasoi-podi-idli.jpg";
            if (name.equals("pongal")) return "/images/food-banners/dakshin-rasoi-pongal.jpg";
            if (name.equals("lemon rice")) return "/images/food-banners/dakshin-rasoi-lemon-rice.jpg";
            if (name.equals("curd rice")) return "/images/food-banners/dakshin-rasoi-curd-rice.jpg";
            if (name.equals("appam with vegetable stew")) return "/images/food-banners/dakshin-rasoi-appam-vegetable-stew.jpg";
            if (name.equals("kerala parotta")) return "/images/food-banners/dakshin-rasoi-kerala-parotta.jpg";
        }

        if (restaurantName.contains("taco fiesta")) {
            if (name.equals("falafel bowl")) return "/images/food-banners/taco-fiesta-falafel-bowl.jpg";
            if (name.equals("hummus with pita")) return "/images/food-banners/taco-fiesta-hummus-with-pita.jpg";
            if (name.equals("cheesy quesadilla")) return "/images/food-banners/taco-fiesta-cheesy-quesadilla.jpg";
            if (name.equals("chipotle chicken burrito")) return "/images/food-banners/taco-fiesta-chipotle-chicken-burrito.jpg";
            if (name.equals("veg frankie")) return "/images/food-banners/taco-fiesta-veg-frankie.jpg";
            if (name.equals("paneer frankie")) return "/images/food-banners/taco-fiesta-paneer-frankie.jpg";
            if (name.equals("chicken frankie")) return "/images/food-banners/taco-fiesta-chicken-frankie.jpg";
            if (name.equals("egg roll")) return "/images/food-banners/taco-fiesta-egg-roll.jpg";
            if (name.equals("chicken kathi roll")) return "/images/food-banners/taco-fiesta-chicken-kathi-roll.jpg";
            if (name.equals("paneer wrap")) return "/images/food-banners/taco-fiesta-paneer-wrap.jpg";
            if (name.equals("chicken shawarma")) return "/images/food-banners/taco-fiesta-chicken-shawarma.jpg";
        }

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

    private static Map<String, String> loadFoodBannerCatalog() {
        try (InputStream input = ImageDefaults.class.getResourceAsStream("/food-banner-catalog.properties")) {
            if (input == null) throw new IllegalStateException("Missing food-banner-catalog.properties");
            Map<String, String> catalog = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    int separator = line.indexOf('=');
                    if (separator > 0) catalog.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
                }
            }
            return Map.copyOf(catalog);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load food banner catalog", e);
        }
    }
}
