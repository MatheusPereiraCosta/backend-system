package com.agendapro.service;

import com.agendapro.domain.Establishment;
import com.agendapro.domain.Professional;
import com.agendapro.domain.ProfessionalWorkingHour;
import com.agendapro.domain.ServiceItem;
import com.agendapro.dto.ProfessionalDTO;
import com.agendapro.dto.WorkingHourDTO;
import com.agendapro.exception.BusinessRuleException;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.ProfessionalRepository;
import com.agendapro.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final EstablishmentService establishmentService;

    @Transactional(readOnly = true)
    public List<ProfessionalDTO> findAll(UUID establishmentId, Boolean activeOnly) {
        List<Professional> list = Boolean.TRUE.equals(activeOnly)
                ? professionalRepository.findByEstablishmentIdAndActiveTrue(establishmentId)
                : professionalRepository.findByEstablishmentId(establishmentId);

        return list.stream().map(ProfessionalDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProfessionalDTO> findByService(UUID establishmentId, UUID serviceId) {
        return professionalRepository.findByEstablishmentIdAndServiceId(establishmentId, serviceId)
                .stream().map(ProfessionalDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionalDTO findById(UUID establishmentId, UUID id) {
        return professionalRepository.findByIdAndEstablishmentId(id, establishmentId)
                .map(ProfessionalDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado com ID: " + id));
    }

    /**
     * Devolve o profissional (somente se for do estabelecimento) com lock de escrita na linha.
     * Deve ser chamado dentro de uma transação, ao criar agendamento, para serializar reservas concorrentes.
     */
    @Transactional
    public Professional getEntityByIdForBooking(UUID establishmentId, UUID id) {
        return professionalRepository.findByIdAndEstablishmentIdForUpdate(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public Professional getEntityById(UUID establishmentId, UUID id) {
        return professionalRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado com ID: " + id));
    }

    /** Aceita apenas serviços do próprio estabelecimento; qualquer ID estranho invalida a requisição. */
    private Set<ServiceItem> resolveServices(UUID establishmentId, List<UUID> serviceIds) {
        Set<UUID> distinct = new HashSet<>(serviceIds);
        List<ServiceItem> services = serviceItemRepository.findByIdInAndEstablishmentId(distinct, establishmentId);
        if (services.size() != distinct.size()) {
            throw new BusinessRuleException("Um ou mais serviços informados não pertencem a este estabelecimento.");
        }
        return new HashSet<>(services);
    }

    /** Atualiza por dia da semana (a tabela tem unicidade profissional+dia, então não dá para apagar e recriar). */
    private void syncWorkingHours(Professional entity, List<WorkingHourDTO> incoming) {
        for (WorkingHourDTO h : incoming) {
            ProfessionalWorkingHour hour = entity.getWorkingHours().stream()
                    .filter(w -> w.getDayOfWeek().equals(h.getDayOfWeek()))
                    .findFirst()
                    .orElseGet(() -> {
                        ProfessionalWorkingHour created = ProfessionalWorkingHour.builder()
                                .professional(entity)
                                .dayOfWeek(h.getDayOfWeek())
                                .build();
                        entity.getWorkingHours().add(created);
                        return created;
                    });
            hour.setEnabled(h.getEnabled() == null || h.getEnabled());
            hour.setStartTime(h.getStartTime());
            hour.setEndTime(h.getEndTime());
            hour.setBreakStart(h.getBreakStart());
            hour.setBreakEnd(h.getBreakEnd());
        }
    }

    @Transactional
    public ProfessionalDTO create(UUID establishmentId, ProfessionalDTO dto) {
        Establishment establishment = establishmentService.getEntityById(establishmentId);

        Professional entity = Professional.builder()
                .establishment(establishment)
                .name(dto.getName())
                .role(dto.getRole())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .avatarUrl(dto.getAvatarUrl())
                .bio(dto.getBio())
                .rating(new BigDecimal("5.00"))
                .totalAppointments(0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            entity.setServices(resolveServices(establishmentId, dto.getServiceIds()));
        }

        if (dto.getWorkingHours() != null && !dto.getWorkingHours().isEmpty()) {
            syncWorkingHours(entity, dto.getWorkingHours());
        }

        return ProfessionalDTO.fromEntity(professionalRepository.save(entity));
    }

    @Transactional
    public ProfessionalDTO update(UUID establishmentId, UUID id, ProfessionalDTO dto) {
        Professional entity = professionalRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado com ID: " + id));

        entity.setName(dto.getName());
        entity.setRole(dto.getRole());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAvatarUrl(dto.getAvatarUrl());
        entity.setBio(dto.getBio());
        if (dto.getActive() != null) entity.setActive(dto.getActive());

        if (dto.getServiceIds() != null) {
            entity.setServices(resolveServices(establishmentId, dto.getServiceIds()));
        }

        if (dto.getWorkingHours() != null && !dto.getWorkingHours().isEmpty()) {
            syncWorkingHours(entity, dto.getWorkingHours());
        }

        return ProfessionalDTO.fromEntity(professionalRepository.save(entity));
    }

    @Transactional
    public void delete(UUID establishmentId, UUID id) {
        Professional entity = professionalRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado com ID: " + id));
        professionalRepository.delete(entity);
    }
}
