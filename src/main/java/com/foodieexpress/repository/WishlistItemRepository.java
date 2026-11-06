package com.foodieexpress.repository;
import com.foodieexpress.model.*;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface WishlistItemRepository extends JpaRepository<WishlistItem,Long>{
 List<WishlistItem> findByCustomerOrderByCreatedAtDesc(Customer customer);
 Optional<WishlistItem> findByCustomerAndFoodItemId(Customer customer,Long foodItemId);
 long deleteByCustomerAndFoodItemId(Customer customer,Long foodItemId);
}
