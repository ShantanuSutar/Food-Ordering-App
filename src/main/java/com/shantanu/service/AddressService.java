package com.shantanu.service;

import com.shantanu.model.Address;
import com.shantanu.model.User;
import com.shantanu.request.AddressRequest;

import java.util.List;

public interface AddressService {
    List<Address> getUserAddresses(User user);
    Address createAddress(AddressRequest request, User user) throws Exception;
    Address updateAddress(Long addressId, AddressRequest request, User user) throws Exception;
    void deleteAddress(Long addressId, User user) throws Exception;
    Address resolveCheckoutAddress(AddressRequest request, User user) throws Exception;
    Address createSnapshot(Address address);
}
