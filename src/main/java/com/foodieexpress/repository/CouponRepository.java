package com.foodieexpress.repository;
import com.foodieexpress.model.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CouponRepository extends JpaRepository<Coupon,Long> {
 Optional<Coupon> findByCodeIgnoreCase(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Coupon c where upper(c.code)=upper(:code)") Optional<Coupon> lockByCode(@Param("code") String code);
 List<Coupon> findAllByOrderByIdDesc();
}
