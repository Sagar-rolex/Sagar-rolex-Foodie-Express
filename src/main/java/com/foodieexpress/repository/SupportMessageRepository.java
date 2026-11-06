package com.foodieexpress.repository;
import com.foodieexpress.model.*;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface SupportMessageRepository extends JpaRepository<SupportMessage,Long>{@EntityGraph(attributePaths="sender") List<SupportMessage> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);}
