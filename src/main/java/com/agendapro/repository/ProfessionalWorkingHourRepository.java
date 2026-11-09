package com.agendapro.repository;

import com.agendapro.domain.ProfessionalWorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessionalWorkingHourRepository extends JpaRepository<ProfessionalWorkingHour, UUID> {
    List<ProfessionalWorkingHour> findByProfessionalId(UUID professionalId);
    Optional<ProfessionalWorkingHour> findByProfessionalIdAndDayOfWeek(UUID professionalId, Short dayOfWeek);
}
