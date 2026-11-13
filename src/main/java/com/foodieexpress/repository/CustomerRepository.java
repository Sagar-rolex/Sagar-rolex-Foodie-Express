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
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser(User user);

    interface CustomerBucket { String getBucket(); Long getTotal(); }
    @Query("select count(c) from Customer c where c.user.createdAt >= :from and c.user.createdAt < :to and c.user.role = :role")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("role") User.Role role);
    @Query(value = "select date_format(u.created_at, :format) as bucket, count(*) as total from customers c join users u on u.id = c.user_id where u.role = 'CUSTOMER' and u.created_at >= :from and u.created_at < :to group by bucket order by bucket", nativeQuery = true)
    List<CustomerBucket> customerGrowthByBucket(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("format") String format);

    @EntityGraph(attributePaths = "user")
    @Query(value = "select c from Customer c join c.user u where u.role = :role and (:search is null or lower(c.fullName) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%')) or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))) and (:active is null or (:active = true and (u.active = true or u.active is null)) or (:active = false and u.active = false))",
            countQuery = "select count(c) from Customer c join c.user u where u.role = :role and (:search is null or lower(c.fullName) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%')) or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))) and (:active is null or (:active = true and (u.active = true or u.active is null)) or (:active = false and u.active = false))")
    Page<Customer> searchForAdmin(@Param("search") String search, @Param("active") Boolean active,
                                  @Param("role") User.Role role, Pageable pageable);
}
