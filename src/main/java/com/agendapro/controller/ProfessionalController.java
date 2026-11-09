package com.agendapro.controller;

import com.agendapro.dto.ProfessionalDTO;
import com.agendapro.service.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/establishments/{establishmentId}/professionals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @GetMapping
    public ResponseEntity<List<ProfessionalDTO>> list(
            @PathVariable UUID establishmentId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return ResponseEntity.ok(professionalService.findAll(establishmentId, activeOnly));
    }

    @GetMapping("/by-service/{serviceId}")
    public ResponseEntity<List<ProfessionalDTO>> listByService(
            @PathVariable UUID establishmentId,
            @PathVariable UUID serviceId) {
        return ResponseEntity.ok(professionalService.findByService(establishmentId, serviceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalDTO> getById(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(professionalService.findById(establishmentId, id));
    }

    @PostMapping
    public ResponseEntity<ProfessionalDTO> create(
            @PathVariable UUID establishmentId,
            @RequestBody @Valid ProfessionalDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(professionalService.create(establishmentId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalDTO> update(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id,
            @RequestBody @Valid ProfessionalDTO dto) {
        return ResponseEntity.ok(professionalService.update(establishmentId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        professionalService.delete(establishmentId, id);
        return ResponseEntity.noContent().build();
    }
}
