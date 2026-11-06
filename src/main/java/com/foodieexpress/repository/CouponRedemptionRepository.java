package com.foodieexpress.repository;
import com.foodieexpress.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption,Long> {
 long countByCouponAndStatusIn(Coupon c, java.util.Collection<CouponRedemption.Status> s);
 long countByCouponAndCustomerAndStatusIn(Coupon c, Customer u, java.util.Collection<CouponRedemption.Status> s);
 java.util.Optional<CouponRedemption> findByOrder(Order order);
}
