package com.agendapro.repository;

import com.agendapro.domain.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    List<ServiceItem> findByEstablishmentId(UUID establishmentId);
    List<ServiceItem> findByEstablishmentIdAndActiveTrue(UUID establishmentId);
    List<ServiceItem> findByEstablishmentIdAndCategoryAndActiveTrue(UUID establishmentId, String category);
    Optional<ServiceItem> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
}
