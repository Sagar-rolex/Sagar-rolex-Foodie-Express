package com.foodieexpress.repository;

import com.foodieexpress.model.FoodItem;
import com.foodieexpress.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    @Query(value = "select f from FoodItem f join f.restaurant r where (:search is null or lower(f.name) like lower(concat('%', :search, '%')) or lower(coalesce(f.category, '')) like lower(concat('%', :search, '%'))) and (:restaurantId is null or r.id = :restaurantId) and (:available is null or f.available = :available)",
            countQuery = "select count(f) from FoodItem f join f.restaurant r where (:search is null or lower(f.name) like lower(concat('%', :search, '%')) or lower(coalesce(f.category, '')) like lower(concat('%', :search, '%'))) and (:restaurantId is null or r.id = :restaurantId) and (:available is null or f.available = :available)")
    Page<FoodItem> searchForAdmin(@Param("search") String search, @Param("restaurantId") Long restaurantId,
                                  @Param("available") Boolean available, Pageable pageable);
    List<FoodItem> findByRestaurant(Restaurant restaurant);
    List<FoodItem> findByRestaurantAndAvailableTrue(Restaurant restaurant);
    List<FoodItem> findByAvailableTrue();
    long countByAvailableTrue();
    Optional<FoodItem> findByRestaurantAndNameIgnoreCase(Restaurant restaurant, String name);
    List<FoodItem> findByCategoryIgnoreCase(String category);
    List<FoodItem> findByNameContainingIgnoreCase(String keyword);
}
