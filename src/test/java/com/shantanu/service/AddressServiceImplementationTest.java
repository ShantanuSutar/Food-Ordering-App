package com.shantanu.service;

import com.shantanu.model.Address;
import com.shantanu.model.Order;
import com.shantanu.model.User;
import com.shantanu.repository.AddressRepository;
import com.shantanu.repository.OrderRepository;
import com.shantanu.repository.UserRepository;
import com.shantanu.request.AddressRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplementationTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AddressServiceImplementation addressService;

    private User user;
    private Address savedAddress;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setAddresses(new ArrayList<>());

        savedAddress = addressEntity(7L);
        user.getAddresses().add(savedAddress);

    }

    @Test
    void rejectsUpdatesForAddressNotOwnedByAuthenticatedUser() {
        Exception error = assertThrows(
                Exception.class,
                () -> addressService.updateAddress(999L, completeAddress(), user)
        );

        assertEquals("Saved address was not found", error.getMessage());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void deletingAddressPreservesLegacyOrderDeliveryHistory() throws Exception {
        Order historicalOrder = new Order();
        historicalOrder.setId(20L);
        historicalOrder.setDeliveryAddress(savedAddress);
        when(orderRepository.findByDeliveryAddressId(7L)).thenReturn(List.of(historicalOrder));
        when(addressRepository.save(any())).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(100L);
            return address;
        });

        addressService.deleteAddress(7L, user);

        assertEquals(0, user.getAddresses().size());
        assertNotSame(savedAddress, historicalOrder.getDeliveryAddress());
        assertEquals("12 Market Road", historicalOrder.getDeliveryAddress().getStreetAddress());
        verify(orderRepository).saveAll(List.of(historicalOrder));
        verify(orderRepository).flush();
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void duplicateAddressIsNotAddedTwice() throws Exception {
        AddressRequest duplicate = completeAddress();
        duplicate.setCity(" pune ");

        Address result = addressService.createAddress(duplicate, user);

        assertEquals(savedAddress, result);
        assertEquals(1, user.getAddresses().size());
        verify(userRepository, never()).save(any());
    }

    @Test
    void editingAddressDoesNotChangeLegacyOrderHistory() throws Exception {
        Order historicalOrder = new Order();
        historicalOrder.setId(21L);
        historicalOrder.setDeliveryAddress(savedAddress);
        when(orderRepository.findByDeliveryAddressId(7L)).thenReturn(List.of(historicalOrder));
        when(addressRepository.save(any())).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            if (address.getId() == null) address.setId(101L);
            return address;
        });

        AddressRequest update = completeAddress();
        update.setStreetAddress("99 New Street");
        addressService.updateAddress(7L, update, user);

        assertEquals("99 New Street", savedAddress.getStreetAddress());
        assertEquals("12 Market Road", historicalOrder.getDeliveryAddress().getStreetAddress());
        assertNotSame(savedAddress, historicalOrder.getDeliveryAddress());
    }

    private AddressRequest completeAddress() {
        AddressRequest address = new AddressRequest();
        address.setFullName("Asha Patil");
        address.setStreetAddress("12 Market Road");
        address.setCity("Pune");
        address.setState("Maharashtra");
        address.setPostalCode("411001");
        address.setCountry("India");
        return address;
    }

    private Address addressEntity(Long id) {
        AddressRequest request = completeAddress();
        Address address = new Address();
        address.setId(id);
        address.setFullName(request.getFullName());
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        return address;
    }
}
