package com.agendapro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private Double revenueGrowthPercentage;
    private Long todayAppointmentsCount;
    private Long monthAppointmentsCount;
    private Double appointmentsGrowthPercentage;
    private Long totalCustomersCount;
    private Long newCustomersThisMonth;
    private Integer occupancyRate;
    private Double cancellationRate;
    private List<AppointmentDTO> upcomingAppointments;
    private RevenueByDayDTO revenueByDay;
    private AppointmentsByStatusDTO appointmentsByStatus;
    private List<TopServiceDTO> topServices;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByDayDTO {
        private List<String> dates;
        private List<SeriesDataDTO> series;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeriesDataDTO {
        private String name;
        private List<BigDecimal> data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentsByStatusDTO {
        private List<String> labels;
        private List<Long> counts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopServiceDTO {
        private String name;
        private Long count;
        private BigDecimal revenue;
    }
}
