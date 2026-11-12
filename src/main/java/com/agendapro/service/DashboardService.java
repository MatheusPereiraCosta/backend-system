package com.agendapro.service;

import com.agendapro.domain.enums.AppointmentStatus;
import com.agendapro.dto.AppointmentDTO;
import com.agendapro.dto.DashboardStatsDTO;
import com.agendapro.repository.AppointmentRepository;
import com.agendapro.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(UUID establishmentId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // Receitas
        BigDecimal todayRevenue = appointmentRepository.sumRevenueByDate(establishmentId, today);
        BigDecimal monthRevenue = appointmentRepository.sumRevenueBetweenDates(establishmentId, startOfMonth, endOfMonth);

        // Contagens
        long todayAppointmentsCount = appointmentRepository
                .countByEstablishmentIdAndAppointmentDateAndStatusNot(establishmentId, today, AppointmentStatus.CANCELLED);
        long monthAppointmentsCount = appointmentRepository
                .countByEstablishmentIdAndAppointmentDateBetweenAndStatusNot(
                        establishmentId, startOfMonth, endOfMonth, AppointmentStatus.CANCELLED);

        long totalCustomersCount = customerRepository.countByEstablishmentId(establishmentId);
        long newCustomersThisMonth = customerRepository
                .countByEstablishmentIdAndCreatedAtAfter(establishmentId,
                        startOfMonth.atStartOfDay(clock.getZone()).toOffsetDateTime());

        // Comparação com o mês anterior (sem dado anterior, o crescimento fica em 0)
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);
        BigDecimal lastMonthRevenue = appointmentRepository.sumRevenueBetweenDates(establishmentId, startOfLastMonth, endOfLastMonth);
        long lastMonthAppointments = appointmentRepository
                .countByEstablishmentIdAndAppointmentDateBetweenAndStatusNot(
                        establishmentId, startOfLastMonth, endOfLastMonth, AppointmentStatus.CANCELLED);
        double revenueGrowth = growthPercentage(monthRevenue, lastMonthRevenue);
        double appointmentsGrowth = growthPercentage(BigDecimal.valueOf(monthAppointmentsCount), BigDecimal.valueOf(lastMonthAppointments));

        long cancelledThisMonth = appointmentRepository
                .countByEstablishmentIdAndAppointmentDateBetweenAndStatus(
                        establishmentId, startOfMonth, endOfMonth, AppointmentStatus.CANCELLED);
        long totalThisMonth = monthAppointmentsCount + cancelledThisMonth;
        double cancellationRate = totalThisMonth == 0 ? 0.0
                : BigDecimal.valueOf(cancelledThisMonth * 100.0 / totalThisMonth).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // Próximos de hoje
        List<AppointmentDTO> upcomingToday = appointmentRepository
                .findByEstablishmentIdAndAppointmentDateOrderByStartTimeAsc(establishmentId, today)
                .stream().map(AppointmentDTO::fromEntity).collect(Collectors.toList());

        // Distribuição de status
        long completed = appointmentRepository.countByEstablishmentIdAndStatus(establishmentId, AppointmentStatus.COMPLETED);
        long confirmed = appointmentRepository.countByEstablishmentIdAndStatus(establishmentId, AppointmentStatus.CONFIRMED);
        long pending = appointmentRepository.countByEstablishmentIdAndStatus(establishmentId, AppointmentStatus.PENDING);
        long cancelled = appointmentRepository.countByEstablishmentIdAndStatus(establishmentId, AppointmentStatus.CANCELLED);

        DashboardStatsDTO.AppointmentsByStatusDTO statusDist = DashboardStatsDTO.AppointmentsByStatusDTO.builder()
                .labels(List.of("Concluídos", "Confirmados", "Pendentes", "Cancelados"))
                .counts(List.of(completed, confirmed, pending, cancelled))
                .build();

        // Últimos 7 dias para o gráfico
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenueByDay = new ArrayList<>();
        List<BigDecimal> countByDay = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dates.add(d.format(dayFormatter));
            BigDecimal rev = appointmentRepository.sumRevenueByDate(establishmentId, d);
            long cnt = appointmentRepository.countByEstablishmentIdAndAppointmentDateAndStatusNot(
                    establishmentId, d, AppointmentStatus.CANCELLED);

            revenueByDay.add(rev);
            countByDay.add(BigDecimal.valueOf(cnt));
        }

        DashboardStatsDTO.RevenueByDayDTO weeklyEvolution = DashboardStatsDTO.RevenueByDayDTO.builder()
                .dates(dates)
                .series(List.of(
                        DashboardStatsDTO.SeriesDataDTO.builder().name("Faturamento (R$)").data(revenueByDay).build(),
                        DashboardStatsDTO.SeriesDataDTO.builder().name("Agendamentos").data(countByDay).build()
                ))
                .build();

        // Top Serviços do mês, a partir dos agendamentos reais
        List<DashboardStatsDTO.TopServiceDTO> topServices = appointmentRepository
                .findTopServices(establishmentId, startOfMonth, endOfMonth, PageRequest.of(0, 4))
                .stream()
                .map(row -> DashboardStatsDTO.TopServiceDTO.builder()
                        .name((String) row[0])
                        .count((Long) row[1])
                        .revenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .revenueGrowthPercentage(revenueGrowth)
                .todayAppointmentsCount(todayAppointmentsCount)
                .monthAppointmentsCount(monthAppointmentsCount)
                .appointmentsGrowthPercentage(appointmentsGrowth)
                .totalCustomersCount(totalCustomersCount)
                .newCustomersThisMonth(newCustomersThisMonth)
                .occupancyRate(0) // TODO: cálculo real de ocupação (minutos agendados / minutos de expediente)
                .cancellationRate(cancellationRate)
                .upcomingAppointments(upcomingToday)
                .revenueByDay(weeklyEvolution)
                .appointmentsByStatus(statusDist)
                .topServices(topServices)
                .build();
    }

    private double growthPercentage(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) return 0.0;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
