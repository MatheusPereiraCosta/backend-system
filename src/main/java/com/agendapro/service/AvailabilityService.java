package com.agendapro.service;

import com.agendapro.domain.Appointment;
import com.agendapro.domain.ProfessionalWorkingHour;
import com.agendapro.domain.ServiceItem;
import com.agendapro.domain.enums.AppointmentStatus;
import com.agendapro.dto.AvailabilitySlotDTO;
import com.agendapro.repository.AppointmentRepository;
import com.agendapro.repository.ProfessionalWorkingHourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ProfessionalWorkingHourRepository workingHourRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceItemService serviceItemService;
    private final ProfessionalService professionalService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AvailabilitySlotDTO calculateAvailableSlots(
            UUID establishmentId,
            UUID professionalId,
            UUID serviceId,
            LocalDate date) {

        // Ambos precisam pertencer ao estabelecimento da URL (isolamento entre estabelecimentos)
        professionalService.getEntityById(establishmentId, professionalId);
        ServiceItem service = serviceItemService.getEntityById(establishmentId, serviceId);
        return computeSlots(establishmentId, professionalId, service, date);
    }

    /** Calcula os horários livres sem revalidar o escopo; o chamador já garantiu que os IDs são do estabelecimento. */
    public AvailabilitySlotDTO computeSlots(
            UUID establishmentId,
            UUID professionalId,
            ServiceItem service,
            LocalDate date) {

        int durationMinutes = service.getDurationMinutes();
        LocalDateTime now = LocalDateTime.now(clock);

        if (date.isBefore(now.toLocalDate())) {
            return AvailabilitySlotDTO.builder().date(date).availableTimes(List.of()).build();
        }

        // 0 = Domingo, 1 = Segunda, ..., 6 = Sábado
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        if (dayOfWeek == 7) {
            dayOfWeek = 0; // Converter Domingo do Java (7) para a nossa convenção (0)
        }

        Optional<ProfessionalWorkingHour> workingHourOpt = workingHourRepository
                .findByProfessionalIdAndDayOfWeek(professionalId, dayOfWeek);

        if (workingHourOpt.isEmpty() || Boolean.FALSE.equals(workingHourOpt.get().getEnabled())) {
            return AvailabilitySlotDTO.builder()
                    .date(date)
                    .availableTimes(List.of())
                    .build();
        }

        ProfessionalWorkingHour workingHour = workingHourOpt.get();
        LocalTime workStart = workingHour.getStartTime();
        LocalTime workEnd = workingHour.getEndTime();
        LocalTime breakStart = workingHour.getBreakStart();
        LocalTime breakEnd = workingHour.getBreakEnd();

        // Buscar agendamentos existentes no dia
        List<Appointment> existingAppointments = appointmentRepository
                .findByEstablishmentIdAndProfessionalIdAndAppointmentDateOrderByStartTimeAsc(
                        establishmentId, professionalId, date);

        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime current = workStart;

        // Intervalos de 30 minutos (ou 15 minutos para serviços curtos)
        int stepMinutes = durationMinutes <= 30 ? 15 : 30;

        while (current.plusMinutes(durationMinutes).isBefore(workEnd) ||
               current.plusMinutes(durationMinutes).equals(workEnd)) {

            LocalTime slotStart = current;
            LocalTime slotEnd = current.plusMinutes(durationMinutes);

            // Verifica se colide com horário de almoço/intervalo
            boolean inBreak = false;
            if (breakStart != null && breakEnd != null) {
                inBreak = (slotStart.isBefore(breakEnd) && slotEnd.isAfter(breakStart));
            }

            // Verifica se colide com agendamentos existentes
            boolean hasCollision = false;
            if (!inBreak) {
                for (Appointment apt : existingAppointments) {
                    if (apt.getStatus() == AppointmentStatus.CANCELLED) continue;

                    boolean overlaps = (slotStart.isBefore(apt.getEndTime()) && slotEnd.isAfter(apt.getStartTime()));
                    if (overlaps) {
                        hasCollision = true;
                        break;
                    }
                }
            }

            // Hoje, não oferece horários que já passaram
            boolean inPast = date.equals(now.toLocalDate()) && !slotStart.isAfter(now.toLocalTime());

            if (!inBreak && !hasCollision && !inPast) {
                availableSlots.add(slotStart);
            }

            current = current.plusMinutes(stepMinutes);
        }

        return AvailabilitySlotDTO.builder()
                .date(date)
                .availableTimes(availableSlots)
                .build();
    }
}
