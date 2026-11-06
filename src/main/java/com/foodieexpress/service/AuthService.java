package com.foodieexpress.service;

import com.foodieexpress.model.User;
import com.foodieexpress.repository.UserRepository;
import com.foodieexpress.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.CUSTOMER);
        return userRepository.save(user);
    }

    public User adminRegister(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.ADMIN);
        return userRepository.save(user);
    }

    public Optional<AuthSession> login(String usernameOrEmail, String password) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!Boolean.FALSE.equals(user.getActive()) && passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(createSession(user));
            }
        }
        return Optional.empty();
    }

    public AuthSession createSession(User user) {
        String token = jwtService.generateToken(user);
        user.setSessionToken(jwtService.fingerprint(token));
        return new AuthSession(userRepository.save(user), token);
    }

    public void logout(String sessionToken) {
        if (!jwtService.isValid(sessionToken)) return;
        userRepository.findBySessionToken(jwtService.fingerprint(sessionToken)).ifPresent(user -> {
            user.setSessionToken(null);
            userRepository.save(user);
        });
    }

    public Optional<User> validateSession(String sessionToken) {
        if (!jwtService.isValid(sessionToken)) return Optional.empty();
        return userRepository.findBySessionToken(jwtService.fingerprint(sessionToken))
                .filter(user -> !Boolean.FALSE.equals(user.getActive()));
    }
}
