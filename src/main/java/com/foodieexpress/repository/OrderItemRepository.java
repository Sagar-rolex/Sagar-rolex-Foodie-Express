package com.foodieexpress.repository;

import com.foodieexpress.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    interface BestSeller { String getName(); Long getQuantity(); }
    @Query(value = "select coalesce(nullif(oi.item_name_at_order, ''), f.name) as name, sum(oi.quantity) as quantity from order_items oi join orders o on o.id = oi.order_id join bills b on b.order_id = o.id join food_items f on f.id = oi.food_item_id where b.payment_status = 'PAID' group by coalesce(nullif(oi.item_name_at_order, ''), f.name) order by quantity desc limit 5", nativeQuery = true)
    List<BestSeller> findBestSellersByPaidQuantity();
}
