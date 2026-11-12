package com.agendapro.service;

import com.agendapro.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration expiration;

    public JwtService(JwtEncoder jwtEncoder, Clock clock,
                      @Value("${app.jwt.expiration-hours:12}") long expirationHours) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.expiration = Duration.ofHours(expirationHours);
    }

    public String generate(User user) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("agendapro")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim("role", user.getRole().name());
        if (user.getEstablishmentId() != null) {
            claims.claim("establishmentId", user.getEstablishmentId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
