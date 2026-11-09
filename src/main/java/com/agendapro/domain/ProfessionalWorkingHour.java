package com.agendapro.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "professional_working_hours", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pro_day_of_week", columnNames = {"professional_id", "day_of_week"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ProfessionalWorkingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek; // 0 = Domingo, 1 = Segunda, ..., 6 = Sábado

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private LocalTime breakStart;

    private LocalTime breakEnd;
}
