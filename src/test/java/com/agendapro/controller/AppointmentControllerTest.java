package com.agendapro.controller;

import com.agendapro.dto.AppointmentDTO;
import com.agendapro.dto.CreateAppointmentRequest;
import com.agendapro.domain.enums.AppointmentStatus;
import com.agendapro.exception.ScheduleConflictException;
import com.agendapro.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("POST /appointments - Deve criar agendamento e retornar 201 Created")
    void shouldCreateAppointmentAndReturn201() throws Exception {
        UUID establishmentId = UUID.randomUUID();
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .professionalId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .customerName("Rafael")
                .customerPhone("11988887777")
                .appointmentDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(14, 0))
                .build();

        AppointmentDTO responseDTO = AppointmentDTO.builder()
                .id(UUID.randomUUID())
                .customerName("Rafael")
                .status(AppointmentStatus.CONFIRMED)
                .price(new BigDecimal("65.00"))
                .build();

        when(appointmentService.create(eq(establishmentId), any(CreateAppointmentRequest.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/establishments/{establishmentId}/appointments", establishmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Rafael"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /appointments - Deve retornar 409 Conflict quando horário já estiver ocupado")
    void shouldReturn409WhenConflict() throws Exception {
        UUID establishmentId = UUID.randomUUID();
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .professionalId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .customerName("Rafael")
                .customerPhone("11988887777")
                .appointmentDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(14, 0))
                .build();

        when(appointmentService.create(eq(establishmentId), any(CreateAppointmentRequest.class)))
                .thenThrow(new ScheduleConflictException("Horário conflitante com outro agendamento."));

        mockMvc.perform(post("/establishments/{establishmentId}/appointments", establishmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de Horário"));
    }
}
