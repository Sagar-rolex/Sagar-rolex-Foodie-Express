package com.foodieexpress.repository;

import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.PaymentRecord;
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

    @Query("select coalesce(sum(b.grandTotal), 0) from Bill b join b.order o join PaymentRecord p on p.order = o where b.paymentStatus = :billStatus and p.status = :paymentStatus and o.status <> :cancelledStatus")
    BigDecimal sumPaidRevenue(@Param("billStatus") Bill.PaymentStatus billStatus,
            @Param("paymentStatus") PaymentRecord.PaymentStatus paymentStatus,
            @Param("cancelledStatus") Order.OrderStatus cancelledStatus);

    @Query("select coalesce(sum(b.grandTotal), 0) from Bill b join b.order o join PaymentRecord p on p.order = o where b.paymentStatus = :billStatus and p.status = :paymentStatus and o.status <> :cancelledStatus and p.updatedAt >= :from and p.updatedAt < :to")
    BigDecimal sumPaidRevenueBetween(@Param("billStatus") Bill.PaymentStatus billStatus,
            @Param("paymentStatus") PaymentRecord.PaymentStatus paymentStatus,
            @Param("cancelledStatus") Order.OrderStatus cancelledStatus,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByPaymentStatus(Bill.PaymentStatus status);

    @Query("select count(p) from PaymentRecord p join Bill b on b.order = p.order join p.order o where b.paymentStatus = :billStatus and p.status = :paymentStatus and o.status <> :cancelledStatus and p.updatedAt >= :from and p.updatedAt < :to")
    long countPaidBetween(@Param("billStatus") Bill.PaymentStatus billStatus,
            @Param("paymentStatus") PaymentRecord.PaymentStatus paymentStatus,
            @Param("cancelledStatus") Order.OrderStatus cancelledStatus,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface RevenueBucket { String getBucket(); BigDecimal getTotal(); }
    @Query(value = "select date_format(p.updated_at, :format) as bucket, sum(b.grand_total) as total from bills b join payment_records p on p.order_id=b.order_id join orders o on o.id=b.order_id where b.payment_status = 'PAID' and p.status='SUCCESS' and o.status <> 'CANCELLED' and p.updated_at >= :from and p.updated_at < :to group by bucket order by bucket", nativeQuery = true)
    List<RevenueBucket> paidRevenueByBucket(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("format") String format);

    interface DailyRevenue { Date getDay(); BigDecimal getTotal(); }
    @Query(value = "select date(generated_at) as day, sum(grand_total) as total from bills where payment_status = 'PAID' and generated_at >= :since group by date(generated_at) order by day", nativeQuery = true)
    List<DailyRevenue> paidRevenueByDaySince(@Param("since") LocalDateTime since);
}
