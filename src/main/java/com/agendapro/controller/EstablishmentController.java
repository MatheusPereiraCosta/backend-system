package com.agendapro.controller;

import com.agendapro.dto.EstablishmentDTO;
import com.agendapro.service.EstablishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/establishments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstablishmentController {

    private final EstablishmentService establishmentService;

    @GetMapping("/{id}")
    public ResponseEntity<EstablishmentDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(establishmentService.findById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<EstablishmentDTO> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(establishmentService.findBySlug(slug));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstablishmentDTO> update(@PathVariable UUID id, @RequestBody @Valid EstablishmentDTO dto) {
        return ResponseEntity.ok(establishmentService.update(id, dto));
    }
}
