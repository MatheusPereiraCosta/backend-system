package com.agendapro.repository;

import com.agendapro.domain.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
    List<Professional> findByEstablishmentId(UUID establishmentId);
    List<Professional> findByEstablishmentIdAndActiveTrue(UUID establishmentId);
    Optional<Professional> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    @Query("SELECT p FROM Professional p JOIN p.services s WHERE p.establishment.id = :establishmentId AND s.id = :serviceId AND p.active = true")
    List<Professional> findByEstablishmentIdAndServiceId(@Param("establishmentId") UUID establishmentId, @Param("serviceId") UUID serviceId);
}
