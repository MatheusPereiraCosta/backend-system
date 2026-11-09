package com.agendapro.service;

import com.agendapro.domain.Establishment;
import com.agendapro.dto.EstablishmentDTO;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.EstablishmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstablishmentService {

    private final EstablishmentRepository establishmentRepository;

    @Transactional(readOnly = true)
    public EstablishmentDTO findById(UUID id) {
        return establishmentRepository.findById(id)
                .map(EstablishmentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public EstablishmentDTO findBySlug(String slug) {
        return establishmentRepository.findBySlug(slug)
                .map(EstablishmentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado com slug: " + slug));
    }

    @Transactional(readOnly = true)
    public Establishment getEntityById(UUID id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado com ID: " + id));
    }

    @Transactional
    public EstablishmentDTO update(UUID id, EstablishmentDTO dto) {
        Establishment entity = getEntityById(id);
        entity.setName(dto.getName());
        entity.setTagline(dto.getTagline());
        entity.setDescription(dto.getDescription());
        entity.setPhone(dto.getPhone());
        entity.setWhatsapp(dto.getWhatsapp());
        entity.setStreet(dto.getStreet());
        entity.setNumber(dto.getNumber());
        entity.setNeighborhood(dto.getNeighborhood());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setBannerUrl(dto.getBannerUrl());
        return EstablishmentDTO.fromEntity(establishmentRepository.save(entity));
    }
}
