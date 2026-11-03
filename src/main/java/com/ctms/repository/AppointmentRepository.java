package com.ctms.repository;

import com.ctms.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDriverIdOrderByAppointmentTimeDesc(Long driverId);
    Optional<Appointment> findByBookingNumber(String bookingNumber);
    boolean existsByBookingNumber(String bookingNumber);
    boolean existsByContainerNumberAndStatusIn(String containerNumber, Collection<Appointment.AppointmentStatus> statuses);
    Optional<Appointment> findFirstByContainerNumberAndStatusInOrderByAppointmentTimeDesc(
            String containerNumber, Collection<Appointment.AppointmentStatus> statuses);
    List<Appointment> findAllByOrderByAppointmentTimeDesc();
    List<Appointment> findByStatus(Appointment.AppointmentStatus status);
}
