package com.agendapro.dto;

import com.agendapro.domain.Professional;
import com.agendapro.domain.ServiceItem;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalDTO {
    private UUID id;

    @NotBlank(message = "O nome do profissional é obrigatório")
    private String name;

    @NotBlank(message = "O cargo/especialidade é obrigatório")
    private String role;

    private String email;

    @NotBlank(message = "O telefone é obrigatório")
    private String phone;

    private String avatarUrl;
    private String bio;
    private BigDecimal rating;
    private Integer totalAppointments;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private List<UUID> serviceIds = new ArrayList<>();

    @Builder.Default
    private List<WorkingHourDTO> workingHours = new ArrayList<>();

    public static ProfessionalDTO fromEntity(Professional entity) {
        if (entity == null) return null;
        return ProfessionalDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .role(entity.getRole())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .avatarUrl(entity.getAvatarUrl())
                .bio(entity.getBio())
                .rating(entity.getRating())
                .totalAppointments(entity.getTotalAppointments())
                .active(entity.getActive())
                .serviceIds(entity.getServices() != null
                        ? entity.getServices().stream().map(ServiceItem::getId).collect(Collectors.toList())
                        : new ArrayList<>())
                .workingHours(entity.getWorkingHours() != null
                        ? entity.getWorkingHours().stream().map(WorkingHourDTO::fromEntity).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
