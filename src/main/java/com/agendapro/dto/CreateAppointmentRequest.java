package com.agendapro.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String customerName;

    @NotBlank(message = "O WhatsApp do cliente é obrigatório")
    @Size(max = 30, message = "O WhatsApp deve ter no máximo 30 caracteres")
    private String customerPhone;

    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    private String customerEmail;

    @NotNull(message = "A data do agendamento é obrigatória")
    private LocalDate appointmentDate;

    @NotNull(message = "O horário de início é obrigatório")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;

    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres")
    private String notes;

    @Builder.Default
    private Boolean isPublicBooking = false;
}
