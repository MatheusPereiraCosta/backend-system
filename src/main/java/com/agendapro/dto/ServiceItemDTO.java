package com.agendapro.dto;

import com.agendapro.domain.ServiceItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemDTO {
    private UUID id;

    @NotBlank(message = "O nome do serviço é obrigatório")
    private String name;

    @NotBlank(message = "A categoria é obrigatória")
    private String category;

    private String description;

    @NotNull(message = "O preço é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    private BigDecimal price;

    @NotNull(message = "A duração é obrigatória")
    @Min(value = 5, message = "A duração mínima é de 5 minutos")
    private Integer durationMinutes;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean popular = false;

    public static ServiceItemDTO fromEntity(ServiceItem entity) {
        if (entity == null) return null;
        return ServiceItemDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .durationMinutes(entity.getDurationMinutes())
                .active(entity.getActive())
                .popular(entity.getPopular())
                .build();
    }
}
