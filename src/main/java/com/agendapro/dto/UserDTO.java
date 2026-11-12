package com.agendapro.dto;

import com.agendapro.domain.User;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String name;
    private String email;
    private String role;
    /** Nulo enquanto o dono ainda não cadastrou o estabelecimento. */
    private UUID establishmentId;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .establishmentId(user.getEstablishmentId())
                .build();
    }
}
