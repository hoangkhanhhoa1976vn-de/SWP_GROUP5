package com.ctms.service;

import com.ctms.model.ShippingLine;
import com.ctms.repository.ShippingLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingLineService {

    private final ShippingLineRepository shippingLineRepository;

    public List<ShippingLine> getAllShippingLines() {
        return shippingLineRepository.findAll();
    }

    public Optional<ShippingLine> findById(Long id) {
        return shippingLineRepository.findById(id);
    }

    @Transactional
    public ShippingLine save(ShippingLine shippingLine) {
        return shippingLineRepository.save(shippingLine);
    }

    @Transactional
    public void delete(Long id) {
        shippingLineRepository.deleteById(id);
    }
}
