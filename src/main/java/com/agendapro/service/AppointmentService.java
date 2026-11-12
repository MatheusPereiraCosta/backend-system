package com.agendapro.service;

import com.agendapro.domain.Appointment;
import com.agendapro.domain.Customer;
import com.agendapro.domain.Establishment;
import com.agendapro.domain.Professional;
import com.agendapro.domain.ServiceItem;
import com.agendapro.domain.enums.AppointmentStatus;
import com.agendapro.dto.AppointmentDTO;
import com.agendapro.dto.CreateAppointmentRequest;
import com.agendapro.exception.BusinessRuleException;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.exception.ScheduleConflictException;
import com.agendapro.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EstablishmentService establishmentService;
    private final ProfessionalService professionalService;
    private final ServiceItemService serviceItemService;
    private final CustomerService customerService;
    private final AvailabilityService availabilityService;

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByDate(UUID establishmentId, LocalDate date) {
        return appointmentRepository
                .findByEstablishmentIdAndAppointmentDateOrderByStartTimeAsc(establishmentId, date)
                .stream().map(AppointmentDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByDateRange(UUID establishmentId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository
                .findByEstablishmentIdAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                        establishmentId, startDate, endDate)
                .stream().map(AppointmentDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByProfessionalAndDate(UUID establishmentId, UUID professionalId, LocalDate date) {
        return appointmentRepository
                .findByEstablishmentIdAndProfessionalIdAndAppointmentDateOrderByStartTimeAsc(
                        establishmentId, professionalId, date)
                .stream().map(AppointmentDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentDTO findById(UUID establishmentId, UUID id) {
        return appointmentRepository.findByIdAndEstablishmentId(id, establishmentId)
                .map(AppointmentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));
    }

    @Transactional
    public AppointmentDTO create(UUID establishmentId, CreateAppointmentRequest request) {
        Establishment establishment = establishmentService.getEntityById(establishmentId);
        // Profissional e serviço precisam ser do estabelecimento da URL. O lock no profissional serializa
        // reservas simultâneas dele: a segunda só roda depois do commit da primeira e então enxerga o conflito.
        Professional professional = professionalService.getEntityByIdForBooking(establishmentId, request.getProfessionalId());
        ServiceItem service = serviceItemService.getEntityById(establishmentId, request.getServiceId());

        if (!Boolean.TRUE.equals(professional.getActive())) {
            throw new BusinessRuleException("Este profissional não está disponível para agendamentos.");
        }
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BusinessRuleException("Este serviço não está disponível para agendamentos.");
        }
        if (professional.getServices().stream().noneMatch(s -> s.getId().equals(service.getId()))) {
            throw new BusinessRuleException("Este profissional não realiza o serviço escolhido.");
        }

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        // 1. Verificação estrita de concorrência / colisão de horários
        List<Appointment> conflicts = appointmentRepository.findOverlappingAppointments(
                professional.getId(),
                request.getAppointmentDate(),
                startTime,
                endTime,
                null
        );

        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(
                    String.format("O profissional %s já possui um agendamento marcado entre %s e %s no dia %s.",
                            professional.getName(), startTime, endTime, request.getAppointmentDate()));
        }

        // 1.1 O horário precisa ser um dos oferecidos (dia útil, dentro do expediente, fora do intervalo e não passado)
        boolean offered = availabilityService
                .computeSlots(establishmentId, professional.getId(), service, request.getAppointmentDate())
                .getAvailableTimes().contains(startTime);
        if (!offered) {
            throw new BusinessRuleException("Horário indisponível. Escolha um dos horários livres exibidos.");
        }

        // 2. Localizar ou cadastrar automaticamente o cliente
        Customer customer = customerService.findOrCreate(
                establishmentId,
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getCustomerEmail()
        );

        // 3. Montar e persistir agendamento
        Appointment appointment = Appointment.builder()
                .establishment(establishment)
                .customer(customer)
                .professional(professional)
                .service(service)
                .appointmentDate(request.getAppointmentDate())
                .startTime(startTime)
                .endTime(endTime)
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .status(AppointmentStatus.CONFIRMED)
                .isPublicBooking(Boolean.TRUE.equals(request.getIsPublicBooking()))
                .notes(request.getNotes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // 4. Atualizar data da última visita do cliente
        customer.setLastVisitAt(OffsetDateTime.now());

        return AppointmentDTO.fromEntity(saved);
    }

    @Transactional
    public AppointmentDTO updateStatus(UUID establishmentId, UUID id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        appointment.setStatus(newStatus);

        if (newStatus == AppointmentStatus.COMPLETED) {
            Professional professional = appointment.getProfessional();
            professional.setTotalAppointments(professional.getTotalAppointments() + 1);
        }

        return AppointmentDTO.fromEntity(appointmentRepository.save(appointment));
    }

    @Transactional
    public void delete(UUID establishmentId, UUID id) {
        Appointment appointment = appointmentRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));
        appointmentRepository.delete(appointment);
    }
}
