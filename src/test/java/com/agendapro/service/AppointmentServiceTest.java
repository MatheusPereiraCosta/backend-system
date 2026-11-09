package com.agendapro.service;

import com.agendapro.domain.Appointment;
import com.agendapro.domain.Customer;
import com.agendapro.domain.Establishment;
import com.agendapro.domain.Professional;
import com.agendapro.domain.ServiceItem;
import com.agendapro.domain.enums.AppointmentStatus;
import com.agendapro.dto.AppointmentDTO;
import com.agendapro.dto.CreateAppointmentRequest;
import com.agendapro.exception.ScheduleConflictException;
import com.agendapro.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private EstablishmentService establishmentService;

    @Mock
    private ProfessionalService professionalService;

    @Mock
    private ServiceItemService serviceItemService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID establishmentId;
    private UUID professionalId;
    private UUID serviceId;
    private Establishment establishment;
    private Professional professional;
    private ServiceItem serviceItem;
    private Customer customer;

    @BeforeEach
    void setUp() {
        establishmentId = UUID.randomUUID();
        professionalId = UUID.randomUUID();
        serviceId = UUID.randomUUID();

        establishment = Establishment.builder().id(establishmentId).name("Barbearia Teste").build();
        professional = Professional.builder().id(professionalId).name("Carlos").build();
        serviceItem = ServiceItem.builder().id(serviceId).name("Corte").price(new BigDecimal("50.00")).durationMinutes(30).build();
        customer = Customer.builder().id(UUID.randomUUID()).name("Lucas").phone("11999998888").build();
    }

    @Test
    @DisplayName("Deve criar um agendamento com sucesso quando não há conflito de horários")
    void shouldCreateAppointmentSuccessfully() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .professionalId(professionalId)
                .serviceId(serviceId)
                .customerName("Lucas")
                .customerPhone("11999998888")
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .build();

        when(establishmentService.getEntityById(establishmentId)).thenReturn(establishment);
        when(professionalService.getEntityById(professionalId)).thenReturn(professional);
        when(serviceItemService.getEntityById(serviceId)).thenReturn(serviceItem);
        when(appointmentRepository.findOverlappingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(customerService.findOrCreate(any(), any(), any(), any())).thenReturn(customer);

        Appointment savedAppointment = Appointment.builder()
                .id(UUID.randomUUID())
                .establishment(establishment)
                .professional(professional)
                .service(serviceItem)
                .customer(customer)
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .endTime(LocalTime.of(10, 30))
                .price(serviceItem.getPrice())
                .durationMinutes(30)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        AppointmentDTO result = appointmentService.create(establishmentId, request);

        assertNotNull(result);
        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        assertEquals("Carlos", result.getProfessionalName());
        assertEquals(new BigDecimal("50.00"), result.getPrice());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar ScheduleConflictException quando há sobreposição de horário para o mesmo profissional")
    void shouldThrowExceptionWhenScheduleConflicts() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .professionalId(professionalId)
                .serviceId(serviceId)
                .customerName("Lucas")
                .customerPhone("11999998888")
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .build();

        when(establishmentService.getEntityById(establishmentId)).thenReturn(establishment);
        when(professionalService.getEntityById(professionalId)).thenReturn(professional);
        when(serviceItemService.getEntityById(serviceId)).thenReturn(serviceItem);

        Appointment conflictingAppointment = Appointment.builder()
                .id(UUID.randomUUID())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .build();

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflictingAppointment));

        assertThrows(ScheduleConflictException.class, () -> appointmentService.create(establishmentId, request));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
