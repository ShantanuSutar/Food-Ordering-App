package com.shantanu.service;

import com.shantanu.model.Address;
import com.shantanu.model.Order;
import com.shantanu.model.User;
import com.shantanu.repository.AddressRepository;
import com.shantanu.repository.OrderRepository;
import com.shantanu.repository.UserRepository;
import com.shantanu.request.AddressRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@Service
public class AddressServiceImplementation implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public List<Address> getUserAddresses(User user) {
        return user.getAddresses();
    }

    @Override
    @Transactional
    public Address createAddress(AddressRequest request, User user) throws Exception {
        Address candidate = toAddress(request);
        validateAddress(candidate);

        Optional<Address> duplicate = user.getAddresses().stream()
                .filter(address -> addressesMatch(address, candidate))
                .findFirst();
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        Address savedAddress = addressRepository.save(candidate);
        user.getAddresses().add(savedAddress);
        userRepository.save(user);
        return savedAddress;
    }

    @Override
    @Transactional
    public Address updateAddress(Long addressId, AddressRequest request, User user) throws Exception {
        Address savedAddress = findOwnedAddress(addressId, user);
        Address candidate = toAddress(request);
        validateAddress(candidate);

        boolean duplicatesAnotherAddress = user.getAddresses().stream()
                .filter(address -> !address.getId().equals(savedAddress.getId()))
                .anyMatch(address -> addressesMatch(address, candidate));
        if (duplicatesAnotherAddress) {
            throw new Exception("An identical address is already saved");
        }

        detachHistoricalOrders(savedAddress);
        copyValues(candidate, savedAddress);
        return addressRepository.save(savedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, User user) throws Exception {
        Address savedAddress = findOwnedAddress(addressId, user);

        detachHistoricalOrders(savedAddress);

        user.getAddresses().removeIf(address -> addressId.equals(address.getId()));
        userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public Address resolveCheckoutAddress(AddressRequest request, User user) throws Exception {
        if (request == null) {
            throw new Exception("Delivery address is required");
        }
        if (request.getId() != null) {
            return updateAddress(request.getId(), request, user);
        }
        return createAddress(request, user);
    }

    @Override
    public Address createSnapshot(Address address) {
        Address snapshot = new Address();
        copyValues(address, snapshot);
        return addressRepository.save(snapshot);
    }

    private Address findOwnedAddress(Long addressId, User user) throws Exception {
        if (addressId == null) {
            throw new Exception("Address id is required");
        }
        return user.getAddresses().stream()
                .filter(address -> addressId.equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new Exception("Saved address was not found"));
    }

    private void detachHistoricalOrders(Address address) {
        // Older orders may share an address-book row. Snapshot the old values
        // before either editing or deleting that current address.
        List<Order> referencingOrders = orderRepository.findByDeliveryAddressId(address.getId());
        for (Order order : referencingOrders) {
            order.setDeliveryAddress(createSnapshot(address));
        }
        if (!referencingOrders.isEmpty()) {
            orderRepository.saveAll(referencingOrders);
            orderRepository.flush();
        }
    }

    private Address toAddress(AddressRequest request) {
        Address address = new Address();
        address.setFullName(normalize(request == null ? null : request.getFullName()));
        address.setStreetAddress(normalize(request == null ? null : request.getStreetAddress()));
        address.setCity(normalize(request == null ? null : request.getCity()));
        address.setState(normalize(request == null ? null : request.getState()));
        address.setPostalCode(normalize(request == null ? null : request.getPostalCode()));
        address.setCountry(normalize(request == null ? null : request.getCountry()));
        return address;
    }

    private void validateAddress(Address address) throws Exception {
        if (isBlank(address.getFullName())
                || isBlank(address.getStreetAddress())
                || isBlank(address.getCity())
                || isBlank(address.getState())
                || isBlank(address.getPostalCode())
                || isBlank(address.getCountry())) {
            throw new Exception("Please provide a complete delivery address");
        }
    }

    private boolean addressesMatch(Address first, Address second) {
        Function<String, String> comparable = value -> normalize(value).toLowerCase(Locale.ROOT);
        return comparable.apply(first.getFullName()).equals(comparable.apply(second.getFullName()))
                && comparable.apply(first.getStreetAddress()).equals(comparable.apply(second.getStreetAddress()))
                && comparable.apply(first.getCity()).equals(comparable.apply(second.getCity()))
                && comparable.apply(first.getState()).equals(comparable.apply(second.getState()))
                && comparable.apply(first.getPostalCode()).equals(comparable.apply(second.getPostalCode()))
                && comparable.apply(first.getCountry()).equals(comparable.apply(second.getCountry()));
    }

    private void copyValues(Address source, Address target) {
        target.setFullName(source.getFullName());
        target.setStreetAddress(source.getStreetAddress());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setPostalCode(source.getPostalCode());
        target.setCountry(source.getCountry());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
