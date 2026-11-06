package com.foodieexpress.service;
import com.foodieexpress.model.User;import org.junit.jupiter.api.Test;import java.util.Optional;import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
class SupportAuthorizationTest {
 @Test void onlyAnAdminCanEnterAdminSupportOperations(){AuthService auth=mock(AuthService.class);AdminAuthorization gate=new AdminAuthorization(auth);User customer=User.builder().id(1L).role(User.Role.CUSTOMER).build();when(auth.validateSession("customer-session")).thenReturn(Optional.of(customer));assertThatThrownBy(()->gate.requireAdmin("customer-session")).hasMessageContaining("Admin access required");User admin=User.builder().id(2L).role(User.Role.ADMIN).build();when(auth.validateSession("admin-session")).thenReturn(Optional.of(admin));assertThat(gate.requireAdmin("admin-session")).isSameAs(admin);}
}
