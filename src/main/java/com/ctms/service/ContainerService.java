package com.ctms.service;

import com.ctms.model.Container;
import com.ctms.model.MovementHistory;
import com.ctms.repository.ContainerRepository;
import com.ctms.repository.MovementHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContainerService {

    private final ContainerRepository containerRepository;
    private final MovementHistoryRepository movementHistoryRepository;

    public List<Container> search(String query,
                                  Container.ContainerType type,
                                  Container.CargoStatus cargoStatus,
                                  Container.Condition condition,
                                  Container.TrackingStatus currentStatus,
                                  Long shippingLineId) {
        if (query != null && query.trim().isEmpty()) {
            query = null;
        }
        return containerRepository.searchContainers(query, type, cargoStatus, condition, currentStatus, shippingLineId);
    }

    public List<Container> findAll() {
        return containerRepository.findAll();
    }

    public List<Container> findByShippingLine(Long shippingLineId) {
        return containerRepository.findByShippingLineId(shippingLineId);
    }

    public Optional<Container> findById(Long id) {
        return containerRepository.findById(id);
    }

    public Optional<Container> findByContainerNumber(String number) {
        return containerRepository.findByContainerNumber(number);
    }

    public List<MovementHistory> getMovementHistory(Long containerId) {
        return movementHistoryRepository.findByContainerIdOrderByTimestampDesc(containerId);
    }

    @Transactional
    public Container save(Container container) {
        return containerRepository.save(container);
    }
}
