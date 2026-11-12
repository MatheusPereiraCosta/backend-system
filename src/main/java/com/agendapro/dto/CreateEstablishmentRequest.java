package com.agendapro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEstablishmentRequest {

    @NotBlank(message = "O nome do estabelecimento é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String name;

    @NotBlank(message = "A categoria é obrigatória")
    @Size(max = 50, message = "A categoria deve ter no máximo 50 caracteres")
    private String category;

    @NotBlank(message = "O número de WhatsApp é obrigatório")
    @Size(max = 30, message = "O WhatsApp deve ter no máximo 30 caracteres")
    private String whatsapp;

    @Size(max = 30)
    private String phone;

    @Size(max = 255, message = "O slogan deve ter no máximo 255 caracteres")
    private String tagline;

    private String description;

    @Size(max = 150)
    private String street;
    @Size(max = 30)
    private String number;
    @Size(max = 100)
    private String neighborhood;
    @Size(max = 100)
    private String city;
    @Size(max = 10)
    private String state;
    @Size(max = 20)
    private String zipCode;
    @Size(max = 500)
    private String logoUrl;
    @Size(max = 500)
    private String bannerUrl;
}
