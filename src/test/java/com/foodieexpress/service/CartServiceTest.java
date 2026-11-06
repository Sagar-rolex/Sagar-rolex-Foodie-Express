package com.foodieexpress.service;

import com.foodieexpress.model.*;
import com.foodieexpress.repository.CartItemRepository;
import com.foodieexpress.repository.CartRepository;
import com.foodieexpress.repository.FoodItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private CartService service;
    private CartRepository carts;
    private CartItemRepository items;
    private FoodItemRepository foods;
    private Customer customer;
    private Cart cart;
    private FoodItem food;

    @BeforeEach
    void setUp() {
        service = new CartService();
        carts = mock(CartRepository.class);
        items = mock(CartItemRepository.class);
        foods = mock(FoodItemRepository.class);
        ReflectionTestUtils.setField(service, "cartRepository", carts);
        ReflectionTestUtils.setField(service, "cartItemRepository", items);
        ReflectionTestUtils.setField(service, "foodItemRepository", foods);
        customer = new Customer(); customer.setId(4L);
        cart = new Cart(); cart.setId(9L); cart.setCustomer(customer); cart.setItems(new ArrayList<>());
        Restaurant restaurant = new Restaurant(); restaurant.setId(2L); restaurant.setActive(true);
        food = new FoodItem(); food.setId(7L); food.setName("Dosa"); food.setPrice(new BigDecimal("125.50"));
        food.setAvailable(true); food.setRestaurant(restaurant);
        when(carts.findByCustomer(customer)).thenReturn(Optional.of(cart));
        when(carts.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));
        when(items.save(any(CartItem.class))).thenAnswer(call -> call.getArgument(0));
        when(foods.findById(7L)).thenReturn(Optional.of(food));
    }

    @Test
    void addingSameFoodItemCombinesQuantityInOneCartLine() {
        service.addItem(customer, 7L, 2);
        service.addItem(customer, 7L, 3);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addRejectsUnavailableFoodAndQuantityAboveMaximum() {
        food.setAvailable(false);
        assertThrows(RuntimeException.class, () -> service.addItem(customer, 7L, 1));
        food.setAvailable(true);
        assertThrows(RuntimeException.class, () -> service.addItem(customer, 7L, 100));
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void quantityUpdateAndRemoveAreScopedToTheCurrentCart() {
        CartItem item = new CartItem(); item.setId(21L); item.setCart(cart); item.setFoodItem(food); item.setQuantity(2);
        cart.addItem(item);
        service.updateQuantity(customer, 21L, 4);
        assertThat(item.getQuantity()).isEqualTo(4);
        service.removeItem(customer, 21L);
        assertThat(cart.getItems()).isEmpty();
        verify(items).delete(item);
    }

    @Test
    void totalUsesCurrentCatalogPriceAndIntegerQuantity() {
        CartItem a = new CartItem(); a.setFoodItem(food); a.setQuantity(2);
        CartItem b = new CartItem(); b.setFoodItem(food); b.setQuantity(1);
        cart.setItems(new ArrayList<>(java.util.List.of(a, b)));
        assertThat(service.calculateTotal(cart)).isEqualByComparingTo("376.50");
    }
}
