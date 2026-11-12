package com.agendapro.dto;

import com.agendapro.domain.Customer;
import com.agendapro.domain.enums.CustomerStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private UUID id;

    @NotBlank(message = "O nome do cliente é obrigatório")
    private String name;

    @NotBlank(message = "O telefone do cliente é obrigatório")
    private String phone;

    private String email;
    private String avatarUrl;

    @Builder.Default
    private CustomerStatus status = CustomerStatus.REGULAR;

    private String notes;

    /** Agendamentos não cancelados. Calculado a partir dos agendamentos, não é gravado no cliente. */
    @Builder.Default
    private Long totalAppointments = 0L;

    /** Soma dos atendimentos concluídos (pagamento é feito no local). */
    @Builder.Default
    private java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;

    private OffsetDateTime lastVisitAt;
    private Long daysSinceLastVisit;
    private OffsetDateTime createdAt;

    public static CustomerDTO fromEntity(Customer entity) {
        if (entity == null) return null;
        Long days = null;
        if (entity.getLastVisitAt() != null) {
            days = ChronoUnit.DAYS.between(entity.getLastVisitAt(), OffsetDateTime.now());
        }

        return CustomerDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .avatarUrl(entity.getAvatarUrl())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .lastVisitAt(entity.getLastVisitAt())
                .daysSinceLastVisit(days)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
