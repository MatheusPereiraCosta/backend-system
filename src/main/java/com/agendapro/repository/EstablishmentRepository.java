package com.agendapro.repository;

import com.agendapro.domain.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    Optional<Establishment> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
