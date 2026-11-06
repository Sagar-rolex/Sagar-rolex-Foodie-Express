package com.foodieexpress.repository;

import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;

import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByOrder(Order order);

    @Query("select coalesce(sum(b.grandTotal), 0) from Bill b where b.paymentStatus = :status")
    BigDecimal sumPaidRevenue(@Param("status") Bill.PaymentStatus status);

    interface DailyRevenue { Date getDay(); BigDecimal getTotal(); }
    @Query(value = "select date(generated_at) as day, sum(grand_total) as total from bills where payment_status = 'PAID' and generated_at >= :since group by date(generated_at) order by day", nativeQuery = true)
    List<DailyRevenue> paidRevenueByDaySince(@Param("since") LocalDateTime since);
}
