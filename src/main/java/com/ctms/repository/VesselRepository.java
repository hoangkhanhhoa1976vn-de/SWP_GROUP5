package com.ctms.repository;

import com.ctms.model.Vessel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VesselRepository extends JpaRepository<Vessel, Long> {
    List<Vessel> findByShippingLineId(Long shippingLineId);
    List<Vessel> findByStatus(Vessel.VesselStatus status);
    List<Vessel> findAllByOrderByEtaAsc();
}
