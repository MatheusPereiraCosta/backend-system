package com.agendapro.service;

import com.agendapro.domain.Customer;
import com.agendapro.domain.Establishment;
import com.agendapro.domain.enums.CustomerStatus;
import com.agendapro.dto.CustomerDTO;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.AppointmentRepository;
import com.agendapro.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final EstablishmentService establishmentService;

    @Transactional(readOnly = true)
    public Page<CustomerDTO> findAll(UUID establishmentId, String search, CustomerStatus status, Pageable pageable) {
        Page<Customer> page;
        if (search != null && !search.isBlank()) {
            page = customerRepository.searchCustomers(establishmentId, search.trim(), pageable);
        } else if (status != null) {
            page = customerRepository.findByEstablishmentIdAndStatus(establishmentId, status, pageable);
        } else {
            page = customerRepository.findByEstablishmentId(establishmentId, pageable);
        }
        List<CustomerDTO> dtos = withTotals(establishmentId, page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<CustomerDTO> findInactive(UUID establishmentId, int daysThreshold) {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(daysThreshold);
        return withTotals(establishmentId, customerRepository.findInactiveCustomers(establishmentId, thresholdDate));
    }

    @Transactional(readOnly = true)
    public CustomerDTO findById(UUID establishmentId, UUID id) {
        Customer customer = customerRepository.findById(id)
                .filter(c -> c.getEstablishment().getId().equals(establishmentId))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        return withTotals(establishmentId, List.of(customer)).get(0);
    }

    /** Converte para DTO preenchendo, em uma única consulta, o total de agendamentos e o total gasto. */
    private List<CustomerDTO> withTotals(UUID establishmentId, List<Customer> customers) {
        if (customers.isEmpty()) return List.of();
        Map<UUID, Object[]> totals = appointmentRepository
                .findCustomerTotals(establishmentId, customers.stream().map(Customer::getId).toList())
                .stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> row));
        return customers.stream().map(c -> {
            CustomerDTO dto = CustomerDTO.fromEntity(c);
            Object[] row = totals.get(c.getId());
            if (row != null) {
                dto.setTotalAppointments((Long) row[1]);
                dto.setTotalSpent((BigDecimal) row[2]);
            }
            return dto;
        }).toList();
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
