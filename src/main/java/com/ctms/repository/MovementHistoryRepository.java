package com.ctms.repository;

import com.ctms.model.MovementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovementHistoryRepository extends JpaRepository<MovementHistory, Long> {
    List<MovementHistory> findByContainerIdOrderByTimestampDesc(Long containerId);
    List<MovementHistory> findTop20ByOrderByTimestampDesc();
}
