package com.ctms.repository;

import com.ctms.model.Berth;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BerthRepository extends JpaRepository<Berth, Long> {
    Optional<Berth> findByBerthCode(String berthCode);
    List<Berth> findByStatus(Berth.BerthStatus status);

    /** Khóa ghi (pessimistic) để không gán hai tàu vào cùng một cầu bến. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Berth b WHERE b.id = :id")
    Optional<Berth> findByIdForUpdate(@Param("id") Long id);
}
