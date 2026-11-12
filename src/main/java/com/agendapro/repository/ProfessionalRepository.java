package com.agendapro.repository;

import com.agendapro.domain.Professional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Trava a linha do profissional até o fim da transação. Usado ao criar agendamento: duas requisições
     * simultâneas para o mesmo profissional são serializadas, então a segunda enxerga a primeira e dá conflito.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Professional p WHERE p.id = :id AND p.establishment.id = :establishmentId")
    Optional<Professional> findByIdAndEstablishmentIdForUpdate(@Param("id") UUID id,
                                                               @Param("establishmentId") UUID establishmentId);

    @Query("SELECT p FROM Professional p JOIN p.services s WHERE p.establishment.id = :establishmentId AND s.id = :serviceId AND p.active = true")
    List<Professional> findByEstablishmentIdAndServiceId(@Param("establishmentId") UUID establishmentId, @Param("serviceId") UUID serviceId);
}
