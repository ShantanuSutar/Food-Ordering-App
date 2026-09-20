package com.shantanu.repository;

import com.shantanu.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCart_Customer_Id(Long id, Long customerId);
}
