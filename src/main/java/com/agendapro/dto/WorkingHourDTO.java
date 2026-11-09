package com.agendapro.dto;

import com.agendapro.domain.ProfessionalWorkingHour;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingHourDTO {
    private UUID id;

    @NotNull(message = "O dia da semana é obrigatório")
    @Min(0) @Max(6)
    private Short dayOfWeek;

    @Builder.Default
    private Boolean enabled = true;

    @NotNull(message = "O horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "O horário de término é obrigatório")
    private LocalTime endTime;

    private LocalTime breakStart;
    private LocalTime breakEnd;

    public static WorkingHourDTO fromEntity(ProfessionalWorkingHour entity) {
        if (entity == null) return null;
        return WorkingHourDTO.builder()
                .id(entity.getId())
                .dayOfWeek(entity.getDayOfWeek())
                .enabled(entity.getEnabled())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .breakStart(entity.getBreakStart())
                .breakEnd(entity.getBreakEnd())
                .build();
    }
}
