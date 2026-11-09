package com.agendapro.controller;

import com.agendapro.dto.AvailabilitySlotDTO;
import com.agendapro.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/establishments/{establishmentId}/availability")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<AvailabilitySlotDTO> getAvailability(
            @PathVariable UUID establishmentId,
            @RequestParam UUID professionalId,
            @RequestParam UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.calculateAvailableSlots(
                establishmentId, professionalId, serviceId, date));
    }
}
