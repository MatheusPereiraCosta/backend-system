package com.agendapro.dto;

import com.agendapro.domain.Establishment;
import lombok.*;

import java.math.BigDecimal;

/** Dados públicos exibidos nos resultados da busca (sem WhatsApp/endereço completo). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstablishmentSummaryDTO {
    private String name;
    private String slug;
    private String tagline;
    private String category;
    private String city;
    private String state;
    private String neighborhood;
    private String logoUrl;
    private BigDecimal rating;
    private Integer totalReviews;

    public static EstablishmentSummaryDTO fromEntity(Establishment e) {
        return EstablishmentSummaryDTO.builder()
                .name(e.getName())
                .slug(e.getSlug())
                .tagline(e.getTagline())
                .category(e.getCategory())
                .city(e.getCity())
                .state(e.getState())
                .neighborhood(e.getNeighborhood())
                .logoUrl(e.getLogoUrl())
                .rating(e.getRating())
                .totalReviews(e.getTotalReviews())
                .build();
    }
}
