package pl.climbbetter.reporting.internal.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.reporting.PeriodSummaryQuery;
import pl.climbbetter.reporting.PeriodSummaryRequest;
import pl.climbbetter.reporting.PeriodTrainingSummary;

@RestController
@RequestMapping("/api/v1/statistics/summary")
class PeriodSummaryController {
    private final PeriodSummaryQuery query;

    PeriodSummaryController(PeriodSummaryQuery query) {
        this.query = query;
    }

    @GetMapping
    PeriodTrainingSummary findSummary(
            @RequestParam(defaultValue = "MONTH") PeriodSummaryRequest.Period period,
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "ALL") PeriodSummaryRequest.Discipline discipline
    ) {
        return query.findSummary(new PeriodSummaryRequest(period, year, month, discipline));
    }
}
