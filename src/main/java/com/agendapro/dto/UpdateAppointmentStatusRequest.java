package com.agendapro.dto;

import com.agendapro.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentStatusRequest {
    @NotNull(message = "O novo status é obrigatório")
    private AppointmentStatus status;
}
