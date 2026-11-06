package com.foodieexpress.repository;
import com.foodieexpress.model.*;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface SupportTicketEventRepository extends JpaRepository<SupportTicketEvent,Long>{@EntityGraph(attributePaths="actor") List<SupportTicketEvent> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);}
