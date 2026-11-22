package pl.climbbetter.reporting.internal.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.reporting.AchievementPyramid;
import pl.climbbetter.reporting.AchievementPyramidQuery;
import pl.climbbetter.reporting.PyramidClimbsPage;
import pl.climbbetter.reporting.PyramidRequest;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/statistics/pyramid")
class AchievementPyramidController {
    private final AchievementPyramidQuery query;
    private final Clock clock;

    AchievementPyramidController(AchievementPyramidQuery query, Clock clock) {
        this.query = query;
        this.clock = clock;
    }

    @GetMapping
    AchievementPyramid findPyramid(
            @RequestParam(defaultValue = "BOULDER") PyramidRequest.View view,
            @RequestParam(defaultValue = "LAST_TWO_YEARS") PyramidRequest.Period period,
            @RequestParam(defaultValue = "RP") PyramidRequest.Style style,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return query.findPyramid(request(view, period, style, asOf));
    }

    @GetMapping("/climbs")
    PyramidClimbsPage findClimbs(
            @RequestParam(defaultValue = "BOULDER") PyramidRequest.View view,
            @RequestParam(defaultValue = "LAST_TWO_YEARS") PyramidRequest.Period period,
            @RequestParam(defaultValue = "RP") PyramidRequest.Style style,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam String bucketKey,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return query.findClimbs(request(view, period, style, asOf), bucketKey, offset, limit);
    }

    private PyramidRequest request(
            PyramidRequest.View view, PyramidRequest.Period period, PyramidRequest.Style style, LocalDate asOf
    ) {
        return new PyramidRequest(view, period, style, asOf == null ? LocalDate.now(clock) : asOf);
    }
}
