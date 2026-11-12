package com.agendapro.repository;

import com.agendapro.domain.Establishment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    Optional<Establishment> findBySlug(String slug);
    boolean existsBySlug(String slug);

    /** Filtros vazios ("") são ignorados. Nunca passe null: o PostgreSQL não infere o tipo do parâmetro. */
    @Query("SELECT e FROM Establishment e WHERE " +
           "(:q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "         OR LOWER(COALESCE(e.tagline, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
           "(:city = '' OR LOWER(COALESCE(e.city, '')) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:category = '' OR LOWER(e.category) = LOWER(:category))")
    Page<Establishment> search(@Param("q") String q,
                               @Param("city") String city,
                               @Param("category") String category,
                               Pageable pageable);
}
