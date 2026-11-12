package com.agendapro.controller;

import com.agendapro.dto.ServiceItemDTO;
import com.agendapro.service.ServiceItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/establishments/{establishmentId}/services")
@RequiredArgsConstructor
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    @GetMapping
    public ResponseEntity<List<ServiceItemDTO>> list(
            @PathVariable UUID establishmentId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return ResponseEntity.ok(serviceItemService.findAll(establishmentId, activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceItemDTO> getById(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(serviceItemService.findById(establishmentId, id));
    }

    @PostMapping
    public ResponseEntity<ServiceItemDTO> create(
            @PathVariable UUID establishmentId,
            @RequestBody @Valid ServiceItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceItemService.create(establishmentId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceItemDTO> update(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id,
            @RequestBody @Valid ServiceItemDTO dto) {
        return ResponseEntity.ok(serviceItemService.update(establishmentId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        serviceItemService.delete(establishmentId, id);
        return ResponseEntity.noContent().build();
    }
}
