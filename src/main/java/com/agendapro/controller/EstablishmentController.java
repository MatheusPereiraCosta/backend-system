package com.agendapro.controller;

import com.agendapro.dto.AuthResponse;
import com.agendapro.dto.CreateEstablishmentRequest;
import com.agendapro.dto.EstablishmentDTO;
import com.agendapro.dto.EstablishmentSummaryDTO;
import com.agendapro.service.EstablishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/establishments")
@RequiredArgsConstructor
public class EstablishmentController {

    private final EstablishmentService establishmentService;

    /** Dono logado cadastra o estabelecimento; a resposta traz um novo token já vinculado a ele. */
    @PostMapping
    public ResponseEntity<AuthResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody @Valid CreateEstablishmentRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(establishmentService.create(userId, request));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EstablishmentSummaryDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 12, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(establishmentService.search(q, city, category, pageable));
    }

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
