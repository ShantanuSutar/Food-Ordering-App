package com.shantanu.repository;

import com.shantanu.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    public List<Order> findByCustomerId(Long userId);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long userId);

    public List<Order> findByRestaurantId(Long restaurantId);

    List<Order> findByDeliveryAddressId(Long addressId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
