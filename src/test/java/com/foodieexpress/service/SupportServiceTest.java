package com.foodieexpress.service;

import com.foodieexpress.dto.SupportRequests;
import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SupportServiceTest {
 private SupportTicketRepository tickets;private SupportMessageRepository messages;private SupportTicketEventRepository events;private FaqEntryRepository faqs;private OrderRepository orders;private UserRepository users;private SupportService service;
 @BeforeEach void setup(){tickets=mock(SupportTicketRepository.class);messages=mock(SupportMessageRepository.class);events=mock(SupportTicketEventRepository.class);faqs=mock(FaqEntryRepository.class);orders=mock(OrderRepository.class);users=mock(UserRepository.class);service=new SupportService(tickets,messages,events,faqs,orders,users);when(tickets.save(any())).thenAnswer(i->i.getArgument(0));when(messages.findByTicketOrderByCreatedAtAsc(any())).thenReturn(List.of());when(events.findByTicketOrderByCreatedAtAsc(any())).thenReturn(List.of());}
 private Customer customer(long id,String username){User u=User.builder().id(id).username(username).email(username+"@test.com").role(User.Role.CUSTOMER).build();return Customer.builder().id(id).fullName(username).user(u).build();}
 private SupportTicket ticket(Customer c,SupportTicket.Status status){return SupportTicket.builder().id(3L).customer(c).subject("Order issue").description("Description").category(SupportTicket.Category.ORDERS).status(status).build();}
 @Test void createsTicketAndStripsHtmlFromCustomerText(){Customer c=customer(1,"sam");var result=service.create(c,new SupportRequests.CreateTicket(SupportTicket.Category.ORDERS,"<b>Order</b> help","<script>alert(1)</script>My order is delayed",null));assertThat(result.status()).isEqualTo(SupportTicket.Status.OPEN);assertThat(result.subject()).isEqualTo("Order help");assertThat(result.description()).doesNotContain("<script>");verify(messages).save(argThat(m->m.getBody().contains("My order is delayed")));verify(events).save(argThat(e->e.getToStatus()==SupportTicket.Status.OPEN&&e.getFromStatus()==null));}
 @Test void rejectsViewingAnotherCustomersTicket(){Customer owner=customer(1,"owner");Customer other=customer(2,"other");when(tickets.findById(3L)).thenReturn(Optional.of(ticket(owner,SupportTicket.Status.OPEN)));assertThatThrownBy(()->service.customerTicket(other,3L)).hasMessageContaining("own support tickets");}
 @Test void rejectsOrderReferenceOwnedByAnotherCustomer(){Customer owner=customer(1,"owner");Customer other=customer(2,"other");Order order=Order.builder().id(22L).customer(owner).build();when(orders.findById(22L)).thenReturn(Optional.of(order));assertThatThrownBy(()->service.create(other,new SupportRequests.CreateTicket(SupportTicket.Category.ORDERS,"Help","Need help",22L))).hasMessageContaining("own orders");}
 @Test void recordsAdminStatusTransition(){Customer c=customer(1,"sam");SupportTicket t=ticket(c,SupportTicket.Status.OPEN);when(tickets.findById(3L)).thenReturn(Optional.of(t));User admin=User.builder().id(10L).username("admin").role(User.Role.ADMIN).build();var result=service.updateStatus(3L,admin,SupportTicket.Status.IN_PROGRESS,"Assigned to support");assertThat(result.status()).isEqualTo(SupportTicket.Status.IN_PROGRESS);verify(events).save(argThat(e->e.getFromStatus()==SupportTicket.Status.OPEN&&e.getToStatus()==SupportTicket.Status.IN_PROGRESS));}
 @Test void rejectsInvalidStatusTransition(){Customer c=customer(1,"sam");when(tickets.findById(3L)).thenReturn(Optional.of(ticket(c,SupportTicket.Status.WAITING_FOR_CUSTOMER)));User admin=User.builder().id(10L).username("admin").role(User.Role.ADMIN).build();assertThatThrownBy(()->service.updateStatus(3L,admin,SupportTicket.Status.OPEN,null)).hasMessageContaining("Invalid support status transition");}
 @Test void reopensResolvedTicketWhenCustomerReplies(){Customer c=customer(1,"sam");SupportTicket t=ticket(c,SupportTicket.Status.RESOLVED);when(tickets.findById(3L)).thenReturn(Optional.of(t));var result=service.customerReply(c,3L,"Still need help");assertThat(result.status()).isEqualTo(SupportTicket.Status.OPEN);verify(events).save(argThat(e->e.getToStatus()==SupportTicket.Status.OPEN));}
 @Test void searchesPublishedFaqQuestionAndAnswer(){when(faqs.findByActiveTrueOrderByCategoryAscIdAsc()).thenReturn(List.of(FaqEntry.builder().id(1L).category(FaqEntry.Category.PAYMENTS).question("Payment failed").answer("Check the payment status").active(true).build()));assertThat(service.searchFaq("payment",null)).hasSize(1);assertThat(service.searchFaq("refund",null)).isEmpty();assertThat(service.searchFaq(null,FaqEntry.Category.PAYMENTS)).hasSize(1);}
}
