package com.ctms.service;

import com.ctms.model.Container;
import com.ctms.model.MovementHistory;
import com.ctms.model.Vessel;
import com.ctms.repository.ContainerRepository;
import com.ctms.repository.MovementHistoryRepository;
import com.ctms.repository.VesselRepository;
import com.ctms.repository.YardSlotRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContainerRepository containerRepository;
    private final YardSlotRepository yardSlotRepository;
    private final VesselRepository vesselRepository;
    private final MovementHistoryRepository movementHistoryRepository;

    public DashboardStats getStats() {
        long totalContainers = containerRepository.count();
        long inYardCount = yardSlotRepository.countByContainerIsNotNull();
        long totalSlots = yardSlotRepository.count();
        long berthedVessels = vesselRepository.findByStatus(Vessel.VesselStatus.BERTHED).size();
        List<MovementHistory> recentMovements = movementHistoryRepository.findTop20ByOrderByTimestampDesc();

        double occupancyRate = totalSlots > 0 ? ((double) inYardCount / totalSlots) * 100.0 : 0.0;

        return DashboardStats.builder()
                .totalContainers(totalContainers)
                .inYardCount(inYardCount)
                .totalSlots(totalSlots)
                .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .berthedVessels(berthedVessels)
                .recentMovements(recentMovements)
                .build();
    }

    @Getter
    @Builder
    public static class DashboardStats {
        private long totalContainers;
        private long inYardCount;
        private long totalSlots;
        private double occupancyRate;
        private long berthedVessels;
        private List<MovementHistory> recentMovements;
    }
}
