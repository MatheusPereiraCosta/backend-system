package com.agendapro.repository;

import com.agendapro.domain.Appointment;
import com.agendapro.domain.enums.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByEstablishmentIdAndAppointmentDateOrderByStartTimeAsc(
            UUID establishmentId, LocalDate appointmentDate);

    List<Appointment> findByEstablishmentIdAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
            UUID establishmentId, LocalDate startDate, LocalDate endDate);

    List<Appointment> findByEstablishmentIdAndProfessionalIdAndAppointmentDateOrderByStartTimeAsc(
            UUID establishmentId, UUID professionalId, LocalDate appointmentDate);

    Optional<Appointment> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    /**
     * Verifica se já existe um agendamento conflitante para o profissional na mesma data.
     * Conflito ocorre quando:
     * (novoInicio >= inicioExistente E novoInicio < fimExistente) OU
     * (novoFim > inicioExistente E novoFim <= fimExistente) OU
     * (novoInicio <= inicioExistente E novoFim >= fimExistente)
     */
    @Query("SELECT a FROM Appointment a WHERE a.professional.id = :professionalId " +
           "AND a.appointmentDate = :appointmentDate " +
           "AND a.status <> 'CANCELLED' " +
           "AND (:excludeId IS NULL OR a.id <> :excludeId) " +
           "AND ((:startTime >= a.startTime AND :startTime < a.endTime) OR " +
           "     (:endTime > a.startTime AND :endTime <= a.endTime) OR " +
           "     (:startTime <= a.startTime AND :endTime >= a.endTime))")
    List<Appointment> findOverlappingAppointments(
            @Param("professionalId") UUID professionalId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    @Query("SELECT COALESCE(SUM(a.price), 0) FROM Appointment a " +
           "WHERE a.establishment.id = :establishmentId " +
           "AND a.appointmentDate = :date " +
           "AND a.status IN ('COMPLETED', 'CONFIRMED', 'IN_PROGRESS')")
    BigDecimal sumRevenueByDate(@Param("establishmentId") UUID establishmentId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(a.price), 0) FROM Appointment a " +
           "WHERE a.establishment.id = :establishmentId " +
           "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
           "AND a.status IN ('COMPLETED', 'CONFIRMED', 'IN_PROGRESS')")
    BigDecimal sumRevenueBetweenDates(@Param("establishmentId") UUID establishmentId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    long countByEstablishmentIdAndAppointmentDateAndStatusNot(
            UUID establishmentId, LocalDate appointmentDate, AppointmentStatus status);

    long countByEstablishmentIdAndAppointmentDateBetweenAndStatusNot(
            UUID establishmentId, LocalDate startDate, LocalDate endDate, AppointmentStatus status);

    long countByEstablishmentIdAndAppointmentDateBetweenAndStatus(
            UUID establishmentId, LocalDate startDate, LocalDate endDate, AppointmentStatus status);

    long countByEstablishmentIdAndStatus(UUID establishmentId, AppointmentStatus status);

    /** Cada linha: [customerId, qtd. de agendamentos não cancelados, total gasto em concluídos]. */
    @Query("SELECT a.customer.id, COUNT(a), " +
           "COALESCE(SUM(CASE WHEN a.status = 'COMPLETED' THEN a.price ELSE 0 END), 0) " +
           "FROM Appointment a " +
           "WHERE a.establishment.id = :establishmentId AND a.customer.id IN :customerIds " +
           "AND a.status <> 'CANCELLED' GROUP BY a.customer.id")
    List<Object[]> findCustomerTotals(@Param("establishmentId") UUID establishmentId,
                                      @Param("customerIds") java.util.Collection<UUID> customerIds);

    /** Cada linha: [nome do serviço, quantidade, faturamento]. Ignora cancelados. */
    @Query("SELECT a.service.name, COUNT(a), COALESCE(SUM(a.price), 0) FROM Appointment a " +
           "WHERE a.establishment.id = :establishmentId " +
           "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
           "AND a.status <> 'CANCELLED' " +
           "GROUP BY a.service.name ORDER BY COUNT(a) DESC")
    List<Object[]> findTopServices(@Param("establishmentId") UUID establishmentId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   Pageable pageable);
}
