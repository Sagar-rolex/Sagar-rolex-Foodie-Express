package com.foodieexpress.service;

import com.foodieexpress.model.Cart;
import com.foodieexpress.model.CartItem;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.FoodItem;
import com.foodieexpress.model.Restaurant;
import com.foodieexpress.repository.CartRepository;
import com.foodieexpress.repository.CartItemRepository;
import com.foodieexpress.repository.FoodItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    public Cart getOrCreateCart(Customer customer) {
        return cartRepository.findByCustomer(customer)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }

    @Transactional
    public Cart addItem(Customer customer, Long foodItemId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new RuntimeException("Quantity must be between 1 and 99");
        }
        Cart cart = getOrCreateCart(customer);
        FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new RuntimeException("Food item not found"));

        if (!foodItem.isAvailable()) {
            throw new RuntimeException("Food item is not available");
        }
        Restaurant restaurant = foodItem.getRestaurant();
        if (restaurant == null || !restaurant.isActive()) {
            throw new RuntimeException("Restaurant is not available");
        }
        if (cart.getItems().stream().anyMatch(i -> !i.getFoodItem().getRestaurant().getId().equals(restaurant.getId()))) {
            throw new RuntimeException("A cart can contain items from one restaurant at a time");
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getFoodItem().getId().equals(foodItemId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int combinedQuantity = item.getQuantity() + quantity;
            if (combinedQuantity > 99) throw new RuntimeException("A maximum of 99 of each item can be ordered");
            item.setQuantity(combinedQuantity);
            item.setPriceAtAdd(foodItem.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setFoodItem(foodItem);
            newItem.setQuantity(quantity);
            newItem.setPriceAtAdd(foodItem.getPrice());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(Customer customer, Long cartItemId) {
        Cart cart = getOrCreateCart(customer);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cart.removeItem(item);
        cartItemRepository.delete(item);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateQuantity(Customer customer, Long cartItemId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new RuntimeException("Quantity must be between 1 and 99");
        }
        Cart cart = getOrCreateCart(customer);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        item.setQuantity(quantity);
        item.setPriceAtAdd(item.getFoodItem().getPrice());
        cartItemRepository.save(item);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart getCart(Customer customer) {
        Cart cart = getOrCreateCart(customer);
        for (CartItem item : cart.getItems()) {
            if (item.getPriceAtAdd() == null) {
                item.setPriceAtAdd(item.getFoodItem().getPrice());
                cartItemRepository.save(item);
            }
        }
        return cart;
    }

    @Transactional
    public com.foodieexpress.dto.CartMergeResponse mergeItems(Customer customer, java.util.List<com.foodieexpress.dto.CartMergeRequest.Item> requestedItems) {
        Cart cart = getOrCreateCart(customer);
        java.util.List<Long> merged = new java.util.ArrayList<>();
        java.util.List<Long> skipped = new java.util.ArrayList<>();
        java.util.List<Long> priceChanged = new java.util.ArrayList<>();
        for (var requested : requestedItems) {
            try {
                FoodItem food = foodItemRepository.findById(requested.foodItemId()).orElse(null);
                if (food == null || !food.isAvailable() || food.getRestaurant() == null || !food.getRestaurant().isActive()) {
                    skipped.add(requested.foodItemId());
                    continue;
                }
                if (requested.priceAtSave() != null && requested.priceAtSave().compareTo(food.getPrice()) != 0) {
                    priceChanged.add(requested.foodItemId());
                }
                CartItem existing = cart.getItems().stream()
                        .filter(i -> i.getFoodItem().getId().equals(requested.foodItemId())).findFirst().orElse(null);
                if (existing != null && existing.getQuantity() + requested.quantity() > 99) {
                    skipped.add(requested.foodItemId());
                    continue;
                }
                if (existing == null && !cart.getItems().isEmpty() && cart.getItems().stream()
                        .anyMatch(i -> !i.getFoodItem().getRestaurant().getId().equals(food.getRestaurant().getId()))) {
                    skipped.add(requested.foodItemId());
                    continue;
                }
                addItem(customer, requested.foodItemId(), requested.quantity());
                merged.add(requested.foodItemId());
            } catch (RuntimeException exception) {
                skipped.add(requested.foodItemId());
            }
        }
        cart = getCart(customer);
        return new com.foodieexpress.dto.CartMergeResponse(cart, calculateTotal(cart), merged, skipped, priceChanged);
    }

    public BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getFoodItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public com.foodieexpress.dto.ReorderResponse addOrderToCart(Customer customer, com.foodieexpress.model.Order order) {
        java.util.List<String> added = new java.util.ArrayList<>();
        java.util.List<String> unavailable = new java.util.ArrayList<>();
        for (com.foodieexpress.model.OrderItem oldItem : order.getItems()) {
            FoodItem food = oldItem.getFoodItem();
            String name = oldItem.getItemNameAtOrder() == null ? (food == null ? "Unavailable item" : food.getName()) : oldItem.getItemNameAtOrder();
            if (food == null || !food.isAvailable() || food.getRestaurant() == null || !food.getRestaurant().isActive()) {
                unavailable.add(name);
                continue;
            }
            try {
                addItem(customer, food.getId(), oldItem.getQuantity());
                added.add(name);
            } catch (RuntimeException e) {
                unavailable.add(name + " (" + e.getMessage() + ")");
            }
        }
        Cart cart = getCart(customer);
        return new com.foodieexpress.dto.ReorderResponse(cart, calculateTotal(cart), added, unavailable);
    }

    @Transactional
    public void clearCart(Customer customer) {
        Cart cart = getOrCreateCart(customer);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
