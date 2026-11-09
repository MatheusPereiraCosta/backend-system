package com.agendapro.dto;

import com.agendapro.domain.Establishment;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstablishmentDTO {
    private UUID id;

    @NotBlank(message = "O nome do estabelecimento é obrigatório")
    private String name;

    @NotBlank(message = "O slug é obrigatório")
    private String slug;

    private String tagline;
    private String description;

    @NotBlank(message = "A categoria é obrigatória")
    private String category;

    private String phone;

    @NotBlank(message = "O número de WhatsApp é obrigatório")
    private String whatsapp;

    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;
    private String zipCode;
    private String logoUrl;
    private String bannerUrl;
    private BigDecimal rating;
    private Integer totalReviews;

    public static EstablishmentDTO fromEntity(Establishment entity) {
        if (entity == null) return null;
        return EstablishmentDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .tagline(entity.getTagline())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .phone(entity.getPhone())
                .whatsapp(entity.getWhatsapp())
                .street(entity.getStreet())
                .number(entity.getNumber())
                .neighborhood(entity.getNeighborhood())
                .city(entity.getCity())
                .state(entity.getState())
                .zipCode(entity.getZipCode())
                .logoUrl(entity.getLogoUrl())
                .bannerUrl(entity.getBannerUrl())
                .rating(entity.getRating())
                .totalReviews(entity.getTotalReviews())
                .build();
    }
}
