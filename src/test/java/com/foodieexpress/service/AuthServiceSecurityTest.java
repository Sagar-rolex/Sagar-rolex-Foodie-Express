package com.foodieexpress.service;

import com.foodieexpress.model.User;
import com.foodieexpress.repository.UserRepository;
import com.foodieexpress.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceSecurityTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService("test-secret-that-is-at-least-32-bytes-long", 86_400_000);
        // inject mock repository and real encoder
        authService = new AuthService();
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "jwtService", jwtService);
    }

    @Test
    void registerNewCustomerHasHashedPasswordAndCustomerRole() {
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setEmail("test@example.com");
        newUser.setPassword("plainpassword123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(newUser);

        assertThat(saved.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(passwordEncoder.matches("plainpassword123", saved.getPassword())).isTrue();
        assertThat(saved.getPassword()).isNotEqualTo("plainpassword123");
    }

    @Test
    void duplicateEmailThrowsException() {
        User existing = new User();
        existing.setEmail("existing@example.com");

        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("existing@example.com");
        newUser.setPassword("password123");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.register(newUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void loginWithCorrectPasswordGeneratesSessionToken() {
        User user = new User();
        user.setUsername("customer");
        user.setId(42L);
        user.setEmail("customer@foodie.com");
        user.setPassword(passwordEncoder.encode("correctpass"));
        user.setRole(User.Role.CUSTOMER);

        when(userRepository.findByUsername("customer@foodie.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("customer@foodie.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<AuthSession> loggedIn = authService.login("customer@foodie.com", "correctpass");

        assertThat(loggedIn).isPresent();
        assertThat(loggedIn.get().token()).isNotBlank();
        assertThat(jwtService.isValid(loggedIn.get().token())).isTrue();
        assertThat(loggedIn.get().user().getSessionToken()).isEqualTo(jwtService.fingerprint(loggedIn.get().token()));
        assertThat(jwtService.isValid(loggedIn.get().token() + "tampered")).isFalse();
        when(userRepository.findBySessionToken(jwtService.fingerprint(loggedIn.get().token())))
                .thenReturn(Optional.of(loggedIn.get().user()), Optional.of(loggedIn.get().user()), Optional.empty());
        assertThat(authService.validateSession(loggedIn.get().token())).contains(loggedIn.get().user());
        authService.logout(loggedIn.get().token());
        assertThat(authService.validateSession(loggedIn.get().token())).isEmpty();
    }

    @Test
    void loginWithIncorrectPasswordFails() {
        User user = new User();
        user.setUsername("customer");
        user.setEmail("customer@foodie.com");
        user.setPassword(passwordEncoder.encode("correctpass"));

        when(userRepository.findByUsername("wrong")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("wrong")).thenReturn(Optional.empty());

        Optional<AuthSession> loggedIn = authService.login("wrong", "wrongpass");

        assertThat(loggedIn).isEmpty();
    }

    @Test
    void adminRegistrationAssignsAdminRole() {
        User adminUser = new User();
        adminUser.setUsername("superadmin");
        adminUser.setEmail("admin@foodie.com");
        adminUser.setPassword("adminpass");

        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@foodie.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.adminRegister(adminUser);

        assertThat(saved.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(passwordEncoder.matches("adminpass", saved.getPassword())).isTrue();
    }
}
