package pl.climbbetter.reporting.internal.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import pl.climbbetter.reporting.PeriodSummaryQuery;
import pl.climbbetter.reporting.PeriodSummaryRequest;
import pl.climbbetter.reporting.PeriodTrainingSummary;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PeriodSummaryController.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(pl.climbbetter.identity.internal.ApiSecurityConfiguration.class)
class PeriodSummaryControllerTest {
    private static final String URL = "/api/v1/statistics/summary";
    @Autowired MockMvc mockMvc;
    @MockitoBean PeriodSummaryQuery query;

    @Test
    void monthlyDefaultsExposeTheRequestedCalendarMonthAndAllDisciplines() throws Exception {
        stubEmptyReport();
        mockMvc.perform(get(URL).param("year", "2024").param("month", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters.period").value("MONTH"))
                .andExpect(jsonPath("$.filters.discipline").value("ALL"))
                .andExpect(jsonPath("$.windowStart").value("2024-02-01"))
                .andExpect(jsonPath("$.windowEnd").value("2024-02-29"));
        verify(query).findSummary(new PeriodSummaryRequest(
                PeriodSummaryRequest.Period.MONTH, 2024, 2, PeriodSummaryRequest.Discipline.ALL));
    }

    @Test
    void explicitYearAndDisciplineArePassedThroughWithoutInventingAMonth() throws Exception {
        stubEmptyReport();
        mockMvc.perform(get(URL).param("period", "YEAR").param("year", "2026")
                        .param("discipline", "ROUTE_AND_CIRCUIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters.period").value("YEAR"))
                .andExpect(jsonPath("$.filters.month").doesNotExist())
                .andExpect(jsonPath("$.filters.discipline").value("ROUTE_AND_CIRCUIT"))
                .andExpect(jsonPath("$.windowStart").value("2026-01-01"))
                .andExpect(jsonPath("$.windowEnd").value("2026-12-31"));
        verify(query).findSummary(new PeriodSummaryRequest(
                PeriodSummaryRequest.Period.YEAR, 2026, null, PeriodSummaryRequest.Discipline.ROUTE_AND_CIRCUIT));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 9998})
    void acceptsSupportedYearBoundaries(int year) throws Exception {
        stubEmptyReport();
        mockMvc.perform(get(URL).param("period", "YEAR").param("year", Integer.toString(year)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters.year").value(year));
        verify(query).findSummary(new PeriodSummaryRequest(
                PeriodSummaryRequest.Period.YEAR, year, null, PeriodSummaryRequest.Discipline.ALL));
    }

    @ParameterizedTest
    @CsvSource({"period,ALL_TIME", "discipline,ROUTES", "year,not-a-year", "year,2147483648",
            "year,0", "year,-1", "year,9999", "month,not-a-month", "month,0", "month,13"})
    void invalidFiltersReturnAProblemBeforeRunningTheQuery(String parameter, String value) throws Exception {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.set("year", "2026");
        parameters.set("month", "11");
        parameters.set(parameter, value);
        mockMvc.perform(get(URL).params(parameters))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_SUMMARY_REQUEST"));
        verifyNoInteractions(query);
    }

    @Test
    void yearIsRequiredRatherThanAssumedFromTheServerClock() throws Exception {
        mockMvc.perform(get(URL).param("month", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_SUMMARY_REQUEST"));
        verifyNoInteractions(query);
    }

    @Test
    void monthlyReportRequiresAMonth() throws Exception {
        mockMvc.perform(get(URL).param("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_SUMMARY_REQUEST"));
        verifyNoInteractions(query);
    }

    @Test
    void yearlyReportRejectsAnAmbiguousMonthParameter() throws Exception {
        mockMvc.perform(get(URL).param("period", "YEAR").param("year", "2026").param("month", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_SUMMARY_REQUEST"));
        verifyNoInteractions(query);
    }

    private void stubEmptyReport() {
        when(query.findSummary(any())).thenAnswer(invocation -> {
            PeriodSummaryRequest request = invocation.getArgument(0);
            return new PeriodTrainingSummary(request, request.windowStart(), request.windowEnd(),
                    new PeriodTrainingSummary.Metrics(0, 0, 0, null, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                            null, null, null, null), 0, 0, List.of(), List.of(), List.of(),
                    new PeriodTrainingSummary.Highlights(null, null,
                            new PeriodTrainingSummary.DisciplineAchievements(null, null, null, null),
                            new PeriodTrainingSummary.DisciplineAchievements(null, null, null, null)));
        });
    }
}
