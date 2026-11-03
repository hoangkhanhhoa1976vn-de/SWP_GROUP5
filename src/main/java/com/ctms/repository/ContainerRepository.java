package com.ctms.repository;

import com.ctms.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByContainerNumber(String containerNumber);

    List<Container> findByShippingLineId(Long shippingLineId);

    List<Container> findByCurrentStatus(Container.TrackingStatus status);

    @Query("SELECT c FROM Container c WHERE " +
           "(:query IS NULL OR LOWER(c.containerNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:containerType IS NULL OR c.containerType = :containerType) AND " +
           "(:cargoStatus IS NULL OR c.cargoStatus = :cargoStatus) AND " +
           "(:condition IS NULL OR c.condition = :condition) AND " +
           "(:currentStatus IS NULL OR c.currentStatus = :currentStatus) AND " +
           "(:shippingLineId IS NULL OR c.shippingLine.id = :shippingLineId)")
    List<Container> searchContainers(
            @Param("query") String query,
            @Param("containerType") Container.ContainerType containerType,
            @Param("cargoStatus") Container.CargoStatus cargoStatus,
            @Param("condition") Container.Condition condition,
            @Param("currentStatus") Container.TrackingStatus currentStatus,
            @Param("shippingLineId") Long shippingLineId
    );
}
