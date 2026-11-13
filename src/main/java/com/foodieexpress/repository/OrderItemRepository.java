package com.foodieexpress.repository;

import com.foodieexpress.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    interface BestSeller { String getName(); Long getQuantity(); }
    interface BestSellerDetail { String getName(); String getRestaurantName(); String getImagePath(); Long getQuantity(); java.math.BigDecimal getRevenue(); }
    @Query(value = "select coalesce(nullif(oi.item_name_at_order, ''), f.name) as name, sum(oi.quantity) as quantity from order_items oi join orders o on o.id = oi.order_id join bills b on b.order_id = o.id join food_items f on f.id = oi.food_item_id where b.payment_status = 'PAID' group by coalesce(nullif(oi.item_name_at_order, ''), f.name) order by quantity desc limit 5", nativeQuery = true)
    List<BestSeller> findBestSellersByPaidQuantity();

    @Query(value = "select coalesce(nullif(oi.item_name_at_order, ''), f.name) as name, r.name as restaurantName, f.image_path as imagePath, sum(oi.quantity) as quantity, sum(oi.price_at_order * oi.quantity) as revenue from order_items oi join orders o on o.id = oi.order_id join bills b on b.order_id = o.id join payment_records p on p.order_id = o.id join food_items f on f.id = oi.food_item_id join restaurants r on r.id = f.restaurant_id where b.payment_status = 'PAID' and p.status='SUCCESS' and o.status <> 'CANCELLED' and p.updated_at >= :from and p.updated_at < :to group by oi.food_item_id, coalesce(nullif(oi.item_name_at_order, ''), f.name), r.name, f.image_path order by quantity desc limit 10", nativeQuery = true)
    List<BestSellerDetail> findBestSellersBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
