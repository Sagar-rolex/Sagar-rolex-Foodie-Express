package com.foodieexpress.repository;

import com.foodieexpress.model.Cart;
import com.foodieexpress.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("select distinct c from Cart c left join fetch c.customer cust left join fetch cust.user " +
            "left join fetch c.items i left join fetch i.foodItem f left join fetch f.restaurant where cust = :customer")
    Optional<Cart> findByCustomer(@Param("customer") Customer customer);
}
