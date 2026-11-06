package com.foodieexpress.repository;

import com.foodieexpress.model.Customer;
import com.foodieexpress.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser(User user);

    @EntityGraph(attributePaths = "user")
    @Query(value = "select c from Customer c join c.user u where u.role = :role and (:search is null or lower(c.fullName) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%')) or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))) and (:active is null or (:active = true and (u.active = true or u.active is null)) or (:active = false and u.active = false))",
            countQuery = "select count(c) from Customer c join c.user u where u.role = :role and (:search is null or lower(c.fullName) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%')) or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))) and (:active is null or (:active = true and (u.active = true or u.active is null)) or (:active = false and u.active = false))")
    Page<Customer> searchForAdmin(@Param("search") String search, @Param("active") Boolean active,
                                  @Param("role") User.Role role, Pageable pageable);
}
