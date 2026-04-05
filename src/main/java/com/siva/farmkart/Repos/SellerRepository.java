package com.siva.farmkart.Repos;

import com.siva.farmkart.Entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
}