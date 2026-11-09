package com.agendapro.repository;

import com.agendapro.domain.Customer;
import com.agendapro.domain.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    Optional<Customer> findByEstablishmentIdAndPhone(UUID establishmentId, String phone);

    Page<Customer> findByEstablishmentId(UUID establishmentId, Pageable pageable);

    Page<Customer> findByEstablishmentIdAndStatus(UUID establishmentId, CustomerStatus status, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.establishment.id = :establishmentId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.phone LIKE CONCAT('%', :search, '%') OR " +
           "(c.email IS NOT NULL AND LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Customer> searchCustomers(@Param("establishmentId") UUID establishmentId,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.establishment.id = :establishmentId AND " +
           "(c.status = 'INACTIVE' OR c.lastVisitAt < :thresholdDate OR c.lastVisitAt IS NULL)")
    List<Customer> findInactiveCustomers(@Param("establishmentId") UUID establishmentId,
                                         @Param("thresholdDate") OffsetDateTime thresholdDate);

    long countByEstablishmentId(UUID establishmentId);
    
    long countByEstablishmentIdAndCreatedAtAfter(UUID establishmentId, OffsetDateTime date);
}
