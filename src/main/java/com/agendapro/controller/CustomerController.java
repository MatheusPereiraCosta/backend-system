package com.agendapro.controller;

import com.agendapro.domain.enums.CustomerStatus;
import com.agendapro.dto.CustomerDTO;
import com.agendapro.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/establishments/{establishmentId}/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> list(
            @PathVariable UUID establishmentId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(customerService.findAll(establishmentId, search, status, pageable));
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<CustomerDTO>> listInactive(
            @PathVariable UUID establishmentId,
            @RequestParam(defaultValue = "30") int daysThreshold) {
        return ResponseEntity.ok(customerService.findInactive(establishmentId, daysThreshold));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getById(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findById(establishmentId, id));
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> create(
            @PathVariable UUID establishmentId,
            @RequestBody @Valid CustomerDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(establishmentId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id,
            @RequestBody @Valid CustomerDTO dto) {
        return ResponseEntity.ok(customerService.update(establishmentId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        customerService.delete(establishmentId, id);
        return ResponseEntity.noContent().build();
    }
}
