package com.foodieexpress.repository;
import com.foodieexpress.model.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long>{
 List<SupportTicket> findByCustomerOrderByUpdatedAtDesc(Customer customer);
 @EntityGraph(attributePaths={"customer","customer.user","order","assignedAdmin"})
 @Query("select t from SupportTicket t where (:status is null or t.status=:status) and (:search is null or lower(t.subject) like lower(concat('%',:search,'%')) or lower(t.customer.fullName) like lower(concat('%',:search,'%')) or (:ticketId is not null and t.id=:ticketId)) order by t.updatedAt desc")
 List<SupportTicket> searchAdmin(@Param("status") SupportTicket.Status status,@Param("search") String search,@Param("ticketId") Long ticketId);
 @Override @EntityGraph(attributePaths={"customer","customer.user","order","assignedAdmin"}) Optional<SupportTicket> findById(Long id);
}
