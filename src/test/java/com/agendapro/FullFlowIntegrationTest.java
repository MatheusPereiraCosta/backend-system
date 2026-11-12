package com.agendapro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de ponta a ponta (Spring Security + JWT + JPA em H2), sem mocks:
 * dono cria conta -> cadastra estabelecimento -> serviço e profissional -> cliente busca, vê horários e agenda.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FullFlowIntegrationTest {

    private static final String PASSWORD = "senha-segura-123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    @DisplayName("Dono cadastra estabelecimento do zero e cliente agenda; conflitos e acessos indevidos são bloqueados")
    void ownerSetupAndCustomerBooking() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Dono cria a conta (ainda sem estabelecimento)
        JsonNode owner = body(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana Dona\",\"email\":\"ana-" + suffix + "@teste.com\",\"password\":\"" + PASSWORD + "\"}"), 201);
        String tokenWithoutEstablishment = owner.get("token").asText();
        assertTrue(owner.get("user").get("establishmentId").isNull());

        // Painel ainda não é acessível: sem estabelecimento vinculado
        mvc.perform(get("/establishments/{id}/services", UUID.randomUUID()).header("Authorization", "Bearer " + tokenWithoutEstablishment))
                .andExpect(status().isOk()); // listagem de serviços é pública
        mvc.perform(get("/establishments/{id}/customers", UUID.randomUUID()).header("Authorization", "Bearer " + tokenWithoutEstablishment))
                .andExpect(status().isForbidden());

        // 2. Cadastra o estabelecimento; slug sem acentos e novo token com o establishmentId
        JsonNode created = body(post("/establishments").header("Authorization", "Bearer " + tokenWithoutEstablishment)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Espaço Café & Cia " + suffix + "\",\"category\":\"Estética\",\"whatsapp\":\"11999990000\",\"city\":\"Campinas\"}"), 201);
        String token = created.get("token").asText();
        String establishmentId = created.get("user").get("establishmentId").asText();
        assertTrue(!establishmentId.isEmpty());

        // Segundo estabelecimento na mesma conta é recusado
        mvc.perform(post("/establishments").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Outro\",\"category\":\"Outros\",\"whatsapp\":\"11999990000\"}"))
                .andExpect(status().isConflict());

        // 3. Serviço e profissional (painel, autenticado)
        JsonNode service = body(post("/establishments/{id}/services", establishmentId).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Limpeza de pele\",\"category\":\"Facial\",\"price\":120.00,\"durationMinutes\":60}"), 201);
        String serviceId = service.get("id").asText();

        StringBuilder hours = new StringBuilder();
        for (int day = 0; day <= 6; day++) {
            boolean workday = day >= 1 && day <= 5;
            if (day > 0) hours.append(',');
            hours.append("{\"dayOfWeek\":").append(day).append(",\"enabled\":").append(workday)
                    .append(",\"startTime\":\"09:00\",\"endTime\":\"18:00\",\"breakStart\":\"12:00\",\"breakEnd\":\"13:00\"}");
        }
        JsonNode professional = body(post("/establishments/{id}/professionals", establishmentId).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Marina\",\"role\":\"Esteticista\",\"phone\":\"11988880000\",\"serviceIds\":[\"" + serviceId
                        + "\"],\"workingHours\":[" + hours + "]}"), 201);
        String professionalId = professional.get("id").asText();
        assertEquals(7, professional.get("workingHours").size(), "os 7 dias da semana precisam ser salvos");

        // 4. Cliente (sem login) encontra pela busca, pelo nome com e sem acento no filtro de cidade/categoria
        mvc.perform(get("/establishments/search").param("q", "café").param("city", "campinas").param("category", "estética"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name=='Espaço Café & Cia " + suffix + "')]").exists());
        mvc.perform(get("/establishments/search").param("q", "inexistente-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        // Busca sem filtros também funciona (parâmetros ausentes)
        mvc.perform(get("/establishments/search")).andExpect(status().isOk());

        JsonNode found = body(get("/establishments/{id}", establishmentId).header("Authorization", "Bearer " + token), 200);
        mvc.perform(get("/establishments/{id}", establishmentId)).andExpect(status().isUnauthorized()); // por ID é só do painel
        String realSlug = found.get("slug").asText();
        assertTrue(realSlug.startsWith("espaco-cafe-cia-"), "slug esperado sem acento/símbolos, veio: " + realSlug);
        mvc.perform(get("/establishments/slug/{slug}", realSlug)).andExpect(status().isOk());

        // 5. Horários livres de uma segunda-feira futura
        LocalDate monday = LocalDate.now().plusDays(7);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) monday = monday.plusDays(1);
        LocalDate sunday = monday.plusDays(6);

        JsonNode slots = body(get("/establishments/{id}/availability", establishmentId)
                .param("professionalId", professionalId).param("serviceId", serviceId).param("date", monday.toString()), 200);
        assertTrue(slots.get("availableTimes").toString().contains("09:00"), "09:00 deve estar livre: " + slots);
        assertTrue(!slots.get("availableTimes").toString().contains("12:00"), "intervalo de almoço não pode ser oferecido");
        assertTrue(!slots.get("availableTimes").toString().contains("12:30"), "serviço de 60min não pode começar no almoço");
        assertEquals(0, body(get("/establishments/{id}/availability", establishmentId)
                .param("professionalId", professionalId).param("serviceId", serviceId).param("date", sunday.toString()), 200)
                .get("availableTimes").size(), "domingo está desabilitado");

        // 6. Cliente agenda sem login
        String booking = "{\"professionalId\":\"" + professionalId + "\",\"serviceId\":\"" + serviceId
                + "\",\"customerName\":\"Carla\",\"customerPhone\":\"11977770000\",\"appointmentDate\":\"" + monday
                + "\",\"startTime\":\"09:00\",\"isPublicBooking\":true}";
        mvc.perform(post("/establishments/{id}/appointments", establishmentId).contentType(MediaType.APPLICATION_JSON).content(booking))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startTime").value("09:00"))
                .andExpect(jsonPath("$.endTime").value("10:00"));

        // Mesmo horário (e sobreposto) -> 409
        mvc.perform(post("/establishments/{id}/appointments", establishmentId).contentType(MediaType.APPLICATION_JSON).content(booking))
                .andExpect(status().isConflict());
        mvc.perform(post("/establishments/{id}/appointments", establishmentId).contentType(MediaType.APPLICATION_JSON)
                        .content(booking.replace("\"09:00\"", "\"09:30\"")))
                .andExpect(status().isConflict());

        // Horário ocupado some da lista
        JsonNode after = body(get("/establishments/{id}/availability", establishmentId)
                .param("professionalId", professionalId).param("serviceId", serviceId).param("date", monday.toString()), 200);
        assertTrue(!after.get("availableTimes").toString().contains("09:00"));

        // Fora do expediente, no almoço, no domingo e no passado -> 400
        for (String invalid : new String[]{
                booking.replace("\"09:00\"", "\"18:00\""),
                booking.replace("\"09:00\"", "\"12:00\""),
                booking.replace(monday.toString(), sunday.toString()),
                booking.replace(monday.toString(), LocalDate.now().minusDays(1).toString())}) {
            mvc.perform(post("/establishments/{id}/appointments", establishmentId).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }

        // 7. Isolamento: outro dono com outro estabelecimento
        JsonNode other = body(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Beto\",\"email\":\"beto-" + suffix + "@teste.com\",\"password\":\"" + PASSWORD + "\"}"), 201);
        JsonNode otherEstablishment = body(post("/establishments").header("Authorization", "Bearer " + other.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Loja do Beto " + suffix + "\",\"category\":\"Outros\",\"whatsapp\":\"11911112222\"}"), 201);
        String otherToken = otherEstablishment.get("token").asText();
        String otherId = otherEstablishment.get("user").get("establishmentId").asText();

        // Beto não acessa o painel da Ana
        mvc.perform(get("/establishments/{id}/customers", establishmentId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/establishments/{id}/dashboard", establishmentId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        // Sem token nenhum, o painel exige login
        mvc.perform(get("/establishments/{id}/customers", establishmentId)).andExpect(status().isUnauthorized());

        // Cliente final não consegue agendar usando profissional/serviço do estabelecimento da Ana no do Beto
        mvc.perform(post("/establishments/{id}/appointments", otherId).contentType(MediaType.APPLICATION_JSON).content(booking))
                .andExpect(status().isNotFound());
        mvc.perform(get("/establishments/{id}/availability", otherId)
                        .param("professionalId", professionalId).param("serviceId", serviceId).param("date", monday.toString()))
                .andExpect(status().isNotFound());

        // Painel da Ana mostra o cliente criado automaticamente pelo agendamento, com totais reais
        mvc.perform(get("/establishments/{id}/customers", establishmentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Carla"))
                .andExpect(jsonPath("$.content[0].totalAppointments").value(1))
                .andExpect(jsonPath("$.content[0].totalSpent").value(0)); // só conta atendimentos concluídos

        // Dashboard do dono usa números reais (sem valores fixos de demonstração)
        JsonNode dashboard = body(get("/establishments/{id}/dashboard", establishmentId).header("Authorization", "Bearer " + token), 200);
        assertEquals(0.0, dashboard.get("revenueGrowthPercentage").asDouble(), "sem mês anterior o crescimento é 0");
        assertEquals(1, dashboard.get("totalCustomersCount").asInt());

        // 8. Login com a senha correta devolve o token já com o estabelecimento; senha errada -> 401
        JsonNode login = body(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana-" + suffix + "@teste.com\",\"password\":\"" + PASSWORD + "\"}"), 200);
        assertEquals(establishmentId, login.get("user").get("establishmentId").asText());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana-" + suffix + "@teste.com\",\"password\":\"errada-errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ANA-" + suffix + "@teste.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Dona"));
    }

    @Test
    @DisplayName("Rotas não declaradas e token inválido são negados")
    void deniesUnknownRoutesAndBadToken() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me").header("Authorization", "Bearer token.invalido.aqui")).andExpect(status().isUnauthorized());
        mvc.perform(get("/qualquer-coisa")).andExpect(status().isUnauthorized());
    }

    private JsonNode body(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request).andReturn();
        assertEquals(expectedStatus, result.getResponse().getStatus(),
                "corpo da resposta: " + result.getResponse().getContentAsString());
        return json.readTree(result.getResponse().getContentAsString());
    }
}
