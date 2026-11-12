package com.agendapro.service;

import com.agendapro.domain.Appointment;
import com.agendapro.domain.Customer;
import com.agendapro.domain.Establishment;
import com.agendapro.domain.Professional;
import com.agendapro.domain.ServiceItem;
import com.agendapro.domain.enums.AppointmentStatus;
import com.agendapro.dto.AppointmentDTO;
import com.agendapro.dto.AvailabilitySlotDTO;
import com.agendapro.dto.CreateAppointmentRequest;
import com.agendapro.exception.BusinessRuleException;
import com.agendapro.exception.ResourceNotFoundException;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private AvailabilityService availabilityService;

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
        serviceItem = ServiceItem.builder().id(serviceId).name("Corte").price(new BigDecimal("50.00")).durationMinutes(30).build();
        professional = Professional.builder().id(professionalId).name("Carlos").services(Set.of(serviceItem)).build();
        customer = Customer.builder().id(UUID.randomUUID()).name("Lucas").phone("11999998888").build();
    }

    private CreateAppointmentRequest request(LocalTime start) {
        return CreateAppointmentRequest.builder()
                .professionalId(professionalId)
                .serviceId(serviceId)
                .customerName("Lucas")
                .customerPhone("11999998888")
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(start)
                .build();
    }

    private void stubScope() {
        when(establishmentService.getEntityById(establishmentId)).thenReturn(establishment);
        when(professionalService.getEntityByIdForBooking(establishmentId, professionalId)).thenReturn(professional);
        when(serviceItemService.getEntityById(establishmentId, serviceId)).thenReturn(serviceItem);
    }

    private void stubSlots(LocalTime... times) {
        when(availabilityService.computeSlots(eq(establishmentId), eq(professionalId), eq(serviceItem), any()))
                .thenReturn(AvailabilitySlotDTO.builder().availableTimes(List.of(times)).build());
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

        stubScope();
        stubSlots(LocalTime.of(10, 0));
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

        stubScope();

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

    @Test
    @DisplayName("Não deve agendar profissional de outro estabelecimento (isolamento)")
    void shouldRejectProfessionalFromAnotherEstablishment() {
        when(establishmentService.getEntityById(establishmentId)).thenReturn(establishment);
        when(professionalService.getEntityByIdForBooking(establishmentId, professionalId))
                .thenThrow(new ResourceNotFoundException("Profissional não encontrado"));

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.create(establishmentId, request(LocalTime.of(10, 0))));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Não deve agendar serviço de outro estabelecimento (isolamento)")
    void shouldRejectServiceFromAnotherEstablishment() {
        when(establishmentService.getEntityById(establishmentId)).thenReturn(establishment);
        when(professionalService.getEntityByIdForBooking(establishmentId, professionalId)).thenReturn(professional);
        when(serviceItemService.getEntityById(establishmentId, serviceId))
                .thenThrow(new ResourceNotFoundException("Serviço não encontrado"));

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.create(establishmentId, request(LocalTime.of(10, 0))));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Não deve agendar quando o profissional não realiza o serviço")
    void shouldRejectWhenProfessionalDoesNotPerformService() {
        professional.setServices(Set.of());
        stubScope();

        assertThrows(BusinessRuleException.class,
                () -> appointmentService.create(establishmentId, request(LocalTime.of(10, 0))));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Não deve agendar profissional inativo")
    void shouldRejectInactiveProfessional() {
        professional.setActive(false);
        stubScope();

        assertThrows(BusinessRuleException.class,
                () -> appointmentService.create(establishmentId, request(LocalTime.of(10, 0))));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Não deve agendar em horário que não está entre os horários livres (fora do expediente, passado ou intervalo)")
    void shouldRejectTimeNotOffered() {
        stubScope();
        stubSlots(LocalTime.of(9, 0));
        when(appointmentRepository.findOverlappingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThrows(BusinessRuleException.class,
                () -> appointmentService.create(establishmentId, request(LocalTime.of(23, 0))));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
