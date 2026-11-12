package com.agendapro.controller;

import com.agendapro.dto.AppointmentDTO;
import com.agendapro.dto.CreateAppointmentRequest;
import com.agendapro.dto.UpdateAppointmentStatusRequest;
import com.agendapro.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/establishments/{establishmentId}/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> listByDate(
            @PathVariable UUID establishmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID professionalId) {

        if (professionalId != null && date != null) {
            return ResponseEntity.ok(appointmentService.findByProfessionalAndDate(establishmentId, professionalId, date));
        }

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(appointmentService.findByDateRange(establishmentId, startDate, endDate));
        }

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(appointmentService.findByDate(establishmentId, targetDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getById(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.findById(establishmentId, id));
    }

    @PostMapping
    public ResponseEntity<AppointmentDTO> create(
            @PathVariable UUID establishmentId,
            @RequestBody @Valid CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(establishmentId, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentDTO> updateStatus(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateAppointmentStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(establishmentId, id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID establishmentId,
            @PathVariable UUID id) {
        appointmentService.delete(establishmentId, id);
        return ResponseEntity.noContent().build();
    }
}
