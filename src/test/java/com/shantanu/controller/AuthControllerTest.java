package com.shantanu.controller;

import com.shantanu.config.JwtProvider;
import com.shantanu.model.Cart;
import com.shantanu.model.USER_ROLE;
import com.shantanu.model.User;
import com.shantanu.repository.CartRepository;
import com.shantanu.repository.UserRepository;
import com.shantanu.request.SignupRequest;
import com.shantanu.response.AuthResponse;
import com.shantanu.service.CustomerUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private CustomerUserDetailsService customerUserDetailsService;
    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    void rejectsAdministratorSelfRegistration() {
        SignupRequest request = validSignup(USER_ROLE.ROLE_ADMIN);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> authController.createUserHandler(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(userRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void issuesSignupTokenWithPersistedOwnerAuthority() throws Exception {
        SignupRequest request = validSignup(USER_ROLE.ROLE_RESTAURANT_OWNER);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(null);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(8L);
            return user;
        });

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "owner@example.com",
                "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT_OWNER"))
        );
        when(customerUserDetailsService.loadUserByUsername("owner@example.com")).thenReturn(userDetails);
        when(jwtProvider.generateToken(argThat(authentication -> authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RESTAURANT_OWNER")))))
                .thenReturn("owner-token");

        ResponseEntity<AuthResponse> response = authController.createUserHandler(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("owner-token", response.getBody().getJwt());
        assertEquals(USER_ROLE.ROLE_RESTAURANT_OWNER, response.getBody().getRole());
        verify(cartRepository).save(argThat(cart -> cart.getCustomer().getId().equals(8L)));
    }

    private SignupRequest validSignup(USER_ROLE role) {
        SignupRequest request = new SignupRequest();
        request.setFullName("Restaurant Owner");
        request.setEmail(" Owner@Example.com ");
        request.setPassword("secret1");
        request.setRole(role);
        return request;
    }
}
