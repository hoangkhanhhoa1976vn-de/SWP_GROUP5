package com.ctms.repository;

import com.ctms.model.ShippingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingLineRepository extends JpaRepository<ShippingLine, Long> {
    Optional<ShippingLine> findByCode(String code);
}
