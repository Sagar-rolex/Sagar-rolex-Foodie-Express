package com.foodieexpress.repository;

import com.foodieexpress.model.Customer;
import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(Customer customer);
    @EntityGraph(attributePaths = {"restaurant"})
    Page<Order> findByCustomerOrderByOrderTimeDesc(Customer customer, Pageable pageable);
    @EntityGraph(attributePaths = {"restaurant"})
    Page<Order> findByCustomerAndStatusOrderByOrderTimeDesc(Customer customer, Order.OrderStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"restaurant"})
    Page<Order> findByCustomerAndStatusInOrderByOrderTimeDesc(Customer customer, List<Order.OrderStatus> statuses, Pageable pageable);
    long countByCustomer(Customer customer);
    java.util.Optional<Order> findFirstByCustomerAndStatusOrderByOrderTimeDesc(Customer customer, Order.OrderStatus status);
    List<Order> findByRestaurant(Restaurant restaurant);

    @Query(value = "select o from Order o join o.customer c join c.user u join o.restaurant r where (:status is null or o.status = :status) and (:restaurantId is null or r.id = :restaurantId) and (:fromDate is null or o.orderTime >= :fromDate) and (:toDate is null or o.orderTime < :toDate) and (:paymentStatus is null or exists (select b.id from Bill b where b.order = o and b.paymentStatus = :paymentStatus)) and (:search is null or lower(c.fullName) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%')) or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%')) or lower(r.name) like lower(concat('%', :search, '%')) or (:orderId is not null and o.id = :orderId))",
            countQuery = "select count(o) from Order o join o.customer c join c.user u join o.restaurant r where (:status is null or o.status = :status) and (:restaurantId is null or r.id = :restaurantId) and (:fromDate is null or o.orderTime >= :fromDate) and (:toDate is null or o.orderTime < :toDate) and (:paymentStatus is null or exists (select b.id from Bill b where b.order = o and b.paymentStatus = :paymentStatus)) and (:search is null or lower(c.fullName) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%')) or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%')) or lower(r.name) like lower(concat('%', :search, '%')) or (:orderId is not null and o.id = :orderId))")
    @EntityGraph(attributePaths = {"customer", "customer.user", "restaurant"})
    Page<Order> searchForAdmin(@Param("search") String search, @Param("status") Order.OrderStatus status,
                               @Param("paymentStatus") Bill.PaymentStatus paymentStatus,
                               @Param("restaurantId") Long restaurantId,
                               @Param("fromDate") java.time.LocalDateTime fromDate,
                               @Param("toDate") java.time.LocalDateTime toDate,
                               @Param("orderId") Long orderId, Pageable pageable);

    long countByStatus(Order.OrderStatus status);
    long countByStatusIn(List<Order.OrderStatus> statuses);
    long countByOrderTimeGreaterThanEqualAndOrderTimeLessThan(java.time.LocalDateTime from, java.time.LocalDateTime to);
    long countByStatusAndOrderTimeGreaterThanEqualAndOrderTimeLessThan(Order.OrderStatus status, java.time.LocalDateTime from, java.time.LocalDateTime to);

    interface OrderBucket { String getBucket(); Long getTotal(); }
    @Query(value = "select date_format(order_time, :format) as bucket, count(*) as total from orders where order_time >= :from and order_time < :to group by bucket order by bucket", nativeQuery = true)
    List<OrderBucket> countOrdersByBucket(@Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to, @Param("format") String format);

    interface DailyCount { java.sql.Date getDay(); Long getTotal(); }
    @Query(value = "select date(order_time) as day, count(*) as total from orders where order_time >= :since group by date(order_time) order by day", nativeQuery = true)
    List<DailyCount> countOrdersByDaySince(@Param("since") java.time.LocalDateTime since);

    @Query("select sum(o.totalAmount) from Order o")
    java.math.BigDecimal sumTotalAmount();

    @EntityGraph(attributePaths = "restaurant")
    List<Order> findTop5ByOrderByOrderTimeDesc();
}
