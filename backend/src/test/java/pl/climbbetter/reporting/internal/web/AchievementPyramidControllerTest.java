package pl.climbbetter.reporting.internal.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.climbbetter.reporting.AchievementPyramid;
import pl.climbbetter.reporting.AchievementPyramidQuery;
import pl.climbbetter.reporting.PyramidRequest;
import pl.climbbetter.training.PyramidLevelEstimator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AchievementPyramidController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(pl.climbbetter.identity.internal.ApiSecurityConfiguration.class)
class AchievementPyramidControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AchievementPyramidQuery query;
    @MockitoBean Clock clock;

    @Test
    void defaultsUseInjectedClockAndExposeEffectiveFilters() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-03T23:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        stubEmptyReport();

        mockMvc.perform(get("/api/v1/statistics/pyramid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters.view").value("BOULDER"))
                .andExpect(jsonPath("$.filters.period").value("LAST_TWO_YEARS"))
                .andExpect(jsonPath("$.filters.style").value("RP"))
                .andExpect(jsonPath("$.filters.asOf").value("2026-11-03"))
                .andExpect(jsonPath("$.windowStart").value("2024-11-03"));
        verify(query).findPyramid(new PyramidRequest(PyramidRequest.View.BOULDER,
                PyramidRequest.Period.LAST_TWO_YEARS, PyramidRequest.Style.RP, LocalDate.of(2026, 11, 3)));
    }

    @Test
    void explicitLocalReferenceDateTakesPrecedenceOverServerClock() throws Exception {
        stubEmptyReport();
        mockMvc.perform(get("/api/v1/statistics/pyramid")
                        .param("view", "ROUTE_AND_CIRCUIT").param("style", "OS")
                        .param("asOf", "2024-02-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters.asOf").value("2024-02-29"))
                .andExpect(jsonPath("$.windowStart").value("2022-02-28"));
        verifyNoInteractions(clock);
    }

    @ParameterizedTest
    @CsvSource({"view,INVALID", "period,LAST_MONTH", "style,FLASH", "asOf,not-a-date", "asOf,2026-02-30"})
    void malformedFiltersReturnAProblemBeforeQuerying(String parameter, String value) throws Exception {
        mockMvc.perform(get("/api/v1/statistics/pyramid").param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PYRAMID_REQUEST"));
        verifyNoInteractions(query);
    }

    @Test
    void bucketKeyIsRequiredForDrillDown() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/pyramid/climbs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PYRAMID_REQUEST"));
        verifyNoInteractions(query);
    }

    private void stubEmptyReport() {
        when(query.findPyramid(any())).thenAnswer(invocation -> {
            PyramidRequest request = invocation.getArgument(0);
            return new AchievementPyramid(request, request.windowStart(), request.asOf(), 0,
                    List.of(), null, PyramidLevelEstimator.estimate(Map.of(), Map.of()), List.of(), null, null);
        });
    }
}
