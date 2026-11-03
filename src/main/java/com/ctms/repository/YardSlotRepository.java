package com.ctms.repository;

import com.ctms.model.Container;
import com.ctms.model.YardSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YardSlotRepository extends JpaRepository<YardSlot, Long> {
    List<YardSlot> findByBlockOrderBySlotIndexAsc(YardSlot.YardBlock block);
    Optional<YardSlot> findBySlotCode(String slotCode);
    Optional<YardSlot> findByContainer(Container container);
    List<YardSlot> findByContainerIsNull();
    long countByContainerIsNotNull();

    /** Khóa ghi (pessimistic) để hai operator không gán trùng một ô bãi. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM YardSlot s WHERE s.id = :id")
    Optional<YardSlot> findByIdForUpdate(@Param("id") Long id);
}
