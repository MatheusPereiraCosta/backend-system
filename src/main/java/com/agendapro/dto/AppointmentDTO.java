package com.agendapro.dto;

import com.agendapro.domain.Appointment;
import com.agendapro.domain.enums.AppointmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDTO {
    private UUID id;
    private UUID establishmentId;
    private UUID customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private UUID professionalId;
    private String professionalName;
    private UUID serviceId;
    private String serviceName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private Integer durationMinutes;
    private AppointmentStatus status;
    private Boolean isPublicBooking;
    private String notes;
    private OffsetDateTime createdAt;

    public static AppointmentDTO fromEntity(Appointment entity) {
        if (entity == null) return null;
        return AppointmentDTO.builder()
                .id(entity.getId())
                .establishmentId(entity.getEstablishment().getId())
                .customerId(entity.getCustomer().getId())
                .customerName(entity.getCustomer().getName())
                .customerPhone(entity.getCustomer().getPhone())
                .customerEmail(entity.getCustomer().getEmail())
                .professionalId(entity.getProfessional().getId())
                .professionalName(entity.getProfessional().getName())
                .serviceId(entity.getService().getId())
                .serviceName(entity.getService().getName())
                .appointmentDate(entity.getAppointmentDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .price(entity.getPrice())
                .durationMinutes(entity.getDurationMinutes())
                .status(entity.getStatus())
                .isPublicBooking(entity.getIsPublicBooking())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
