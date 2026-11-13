package com.foodieexpress.repository;

import com.foodieexpress.model.Order;
import com.foodieexpress.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentRecord p where p.razorpayOrderId = :orderId")
    Optional<PaymentRecord> findByRazorpayOrderIdForUpdate(@Param("orderId") String razorpayOrderId);

    Optional<PaymentRecord> findByOrder(Order order);
}
