package com.agendapro.service;

import com.agendapro.domain.Customer;
import com.agendapro.domain.Establishment;
import com.agendapro.domain.enums.CustomerStatus;
import com.agendapro.dto.CustomerDTO;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final EstablishmentService establishmentService;

    @Transactional(readOnly = true)
    public Page<CustomerDTO> findAll(UUID establishmentId, String search, CustomerStatus status, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return customerRepository.searchCustomers(establishmentId, search.trim(), pageable)
                    .map(CustomerDTO::fromEntity);
        }
        if (status != null) {
            return customerRepository.findByEstablishmentIdAndStatus(establishmentId, status, pageable)
                    .map(CustomerDTO::fromEntity);
        }
        return customerRepository.findByEstablishmentId(establishmentId, pageable)
                .map(CustomerDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CustomerDTO> findInactive(UUID establishmentId, int daysThreshold) {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(daysThreshold);
        return customerRepository.findInactiveCustomers(establishmentId, thresholdDate)
                .stream().map(CustomerDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerDTO findById(UUID establishmentId, UUID id) {
        return customerRepository.findById(id)
                .filter(c -> c.getEstablishment().getId().equals(establishmentId))
                .map(CustomerDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
    }

    @Transactional
    public Customer findOrCreate(UUID establishmentId, String name, String phone, String email) {
        return customerRepository.findByEstablishmentIdAndPhone(establishmentId, phone)
                .orElseGet(() -> {
                    Establishment establishment = establishmentService.getEntityById(establishmentId);
                    Customer newCustomer = Customer.builder()
                            .establishment(establishment)
                            .name(name)
                            .phone(phone)
                            .email(email)
                            .status(CustomerStatus.REGULAR)
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }

    @Transactional
    public CustomerDTO create(UUID establishmentId, CustomerDTO dto) {
        Establishment establishment = establishmentService.getEntityById(establishmentId);
        Customer entity = Customer.builder()
                .establishment(establishment)
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .avatarUrl(dto.getAvatarUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : CustomerStatus.REGULAR)
                .notes(dto.getNotes())
                .build();

        return CustomerDTO.fromEntity(customerRepository.save(entity));
    }

    @Transactional
    public CustomerDTO update(UUID establishmentId, UUID id, CustomerDTO dto) {
        Customer entity = customerRepository.findById(id)
                .filter(c -> c.getEstablishment().getId().equals(establishmentId))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));

        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setNotes(dto.getNotes());

        return CustomerDTO.fromEntity(customerRepository.save(entity));
    }

    @Transactional
    public void delete(UUID establishmentId, UUID id) {
        Customer entity = customerRepository.findById(id)
                .filter(c -> c.getEstablishment().getId().equals(establishmentId))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        customerRepository.delete(entity);
    }
}
