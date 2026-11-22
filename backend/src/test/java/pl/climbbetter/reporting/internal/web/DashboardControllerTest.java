package pl.climbbetter.reporting.internal.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.reporting.DashboardSessionSummary;
import pl.climbbetter.reporting.DashboardSessionsPage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(pl.climbbetter.identity.internal.ApiSecurityConfiguration.class)
class DashboardControllerTest {

    @Test
    void returnsAPageAndPassesCursorToQuery() throws Exception {
        when(dashboardQuery.findSessionsPage(20, "next")).thenReturn(new DashboardSessionsPage(List.of(), null));
        mockMvc.perform(get("/api/v1/dashboard/sessions/page").param("cursor", "next"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    @Test
    void invalidPaginationReturnsBadRequest() throws Exception {
        when(dashboardQuery.findSessionsPage(20, "bad")).thenThrow(new IllegalArgumentException("Invalid session cursor"));
        mockMvc.perform(get("/api/v1/dashboard/sessions/page").param("cursor", "bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DashboardQuery dashboardQuery;

    @Test
    void returnsEmptyListWhenThereAreNoSessions() throws Exception {
        when(dashboardQuery.findRecentSessions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/sessions"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returnsRecentSessions() throws Exception {
        UUID sessionId = UUID.fromString("d1bf71b2-e949-4d15-8258-b307f9302f29");

        DashboardSessionSummary session = new DashboardSessionSummary(
                sessionId,
                "Evening training",
                LocalDate.of(2026, 10, 4),
                "My Gym",
                90,
                120,
                new BigDecimal("850.00"),
                new BigDecimal("920.50"),
                new BigDecimal("12.5000000000"),
                "7A",
                "7c");

        when(dashboardQuery.findRecentSessions()).thenReturn(List.of(session));

        mockMvc.perform(get("/api/v1/dashboard/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].sessionName").value("Evening training"))
                .andExpect(jsonPath("$[0].sessionDate").value("2026-10-04"))
                .andExpect(jsonPath("$[0].classicLoad").value(850.00))
                .andExpect(jsonPath("$[0].adjustedLoad").value(920.50))
                .andExpect(
                        jsonPath("$[0].averageMoveIntensity")
                                .value(12.5)
                )
                .andExpect(jsonPath("$[0].maxBoulderGrade").value("7A"))
                .andExpect(jsonPath("$[0].maxRouteGrade").value("7c"));
    }
}
