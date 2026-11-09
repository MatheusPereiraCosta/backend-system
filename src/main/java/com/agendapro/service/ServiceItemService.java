package com.agendapro.service;

import com.agendapro.domain.Establishment;
import com.agendapro.domain.ServiceItem;
import com.agendapro.dto.ServiceItemDTO;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final EstablishmentService establishmentService;

    @Transactional(readOnly = true)
    public List<ServiceItemDTO> findAll(UUID establishmentId, Boolean activeOnly) {
        List<ServiceItem> list = Boolean.TRUE.equals(activeOnly)
                ? serviceItemRepository.findByEstablishmentIdAndActiveTrue(establishmentId)
                : serviceItemRepository.findByEstablishmentId(establishmentId);

        return list.stream().map(ServiceItemDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceItemDTO findById(UUID establishmentId, UUID id) {
        return serviceItemRepository.findByIdAndEstablishmentId(id, establishmentId)
                .map(ServiceItemDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public ServiceItem getEntityById(UUID id) {
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
    }

    @Transactional
    public ServiceItemDTO create(UUID establishmentId, ServiceItemDTO dto) {
        Establishment establishment = establishmentService.getEntityById(establishmentId);
        ServiceItem entity = ServiceItem.builder()
                .establishment(establishment)
                .name(dto.getName())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .durationMinutes(dto.getDurationMinutes())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .popular(dto.getPopular() != null ? dto.getPopular() : false)
                .build();

        return ServiceItemDTO.fromEntity(serviceItemRepository.save(entity));
    }

    @Transactional
    public ServiceItemDTO update(UUID establishmentId, UUID id, ServiceItemDTO dto) {
        ServiceItem entity = serviceItemRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));

        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setDescription(dto.getDescription());
        entity.setPrice(dto.getPrice());
        entity.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
        if (dto.getPopular() != null) entity.setPopular(dto.getPopular());

        return ServiceItemDTO.fromEntity(serviceItemRepository.save(entity));
    }

    @Transactional
    public void delete(UUID establishmentId, UUID id) {
        ServiceItem entity = serviceItemRepository.findByIdAndEstablishmentId(id, establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
        serviceItemRepository.delete(entity);
    }
}
