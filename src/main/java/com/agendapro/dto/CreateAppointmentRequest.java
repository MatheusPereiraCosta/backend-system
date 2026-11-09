package com.agendapro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentRequest {

    @NotNull(message = "O ID do profissional é obrigatório")
    private UUID professionalId;

    @NotNull(message = "O ID do serviço é obrigatório")
    private UUID serviceId;

    @NotBlank(message = "O nome do cliente é obrigatório")
    private String customerName;

    @NotBlank(message = "O WhatsApp do cliente é obrigatório")
    private String customerPhone;

    private String customerEmail;

    @NotNull(message = "A data do agendamento é obrigatória")
    private LocalDate appointmentDate;

    @NotNull(message = "O horário de início é obrigatório")
    private LocalTime startTime;

    private String notes;

    @Builder.Default
    private Boolean isPublicBooking = false;
}
