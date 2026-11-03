package com.ctms.service;

import com.ctms.model.Berth;
import com.ctms.model.Vessel;
import com.ctms.repository.BerthRepository;
import com.ctms.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BerthService {

    private final BerthRepository berthRepository;
    private final VesselRepository vesselRepository;

    public List<Berth> getAllBerths() {
        return berthRepository.findAll();
    }

    public List<Vessel> getAllVessels() {
        return vesselRepository.findAllByOrderByEtaAsc();
    }

    public List<Vessel> getVesselsByShippingLine(Long shippingLineId) {
        return vesselRepository.findByShippingLineId(shippingLineId);
    }

    public Optional<Berth> findBerthById(Long id) {
        return berthRepository.findById(id);
    }

    public Optional<Vessel> findVesselById(Long id) {
        return vesselRepository.findById(id);
    }

    /**
     * Use Case: Assign Vessel to Berth
     * <<include>> Update Berth / Vessel Status
     */
    @Transactional
    public void assignVesselToBerth(Long vesselId, Long berthId) {
        Vessel vessel = vesselRepository.findById(vesselId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu ID: " + vesselId));
        Berth berth = berthRepository.findByIdForUpdate(berthId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cầu bến ID: " + berthId));

        if (berth.getStatus() == Berth.BerthStatus.OCCUPIED &&
                berth.getCurrentVessel() != null &&
                !berth.getCurrentVessel().getId().equals(vesselId)) {
            throw new IllegalStateException("Cầu cảng " + berth.getBerthCode() + " hiện đang có tàu " +
                    berth.getCurrentVessel().getName() + " neo đậu!");
        }

        // Nếu tàu này đang ở cầu cảng khác thì giải phóng cầu cảng cũ
        if (vessel.getAssignedBerth() != null && !vessel.getAssignedBerth().getId().equals(berthId)) {
            Berth oldBerth = vessel.getAssignedBerth();
            oldBerth.setStatus(Berth.BerthStatus.AVAILABLE);
            oldBerth.setCurrentVessel(null);
            berthRepository.save(oldBerth);
        }

        // <<include>> Update Berth / Vessel Status
        berth.setStatus(Berth.BerthStatus.OCCUPIED);
        berth.setCurrentVessel(vessel);
        berthRepository.save(berth);

        vessel.setStatus(Vessel.VesselStatus.BERTHED);
        vessel.setAssignedBerth(berth);
        vesselRepository.save(vessel);
    }

    @Transactional
    public void departVessel(Long vesselId) {
        Vessel vessel = vesselRepository.findById(vesselId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tàu ID: " + vesselId));

        if (vessel.getAssignedBerth() != null) {
            Berth berth = vessel.getAssignedBerth();
            berth.setStatus(Berth.BerthStatus.AVAILABLE);
            berth.setCurrentVessel(null);
            berthRepository.save(berth);
        }

        vessel.setStatus(Vessel.VesselStatus.DEPARTED);
        vessel.setAssignedBerth(null);
        vesselRepository.save(vessel);
    }

    @Transactional
    public Vessel saveVessel(Vessel vessel) {
        return vesselRepository.save(vessel);
    }
}
