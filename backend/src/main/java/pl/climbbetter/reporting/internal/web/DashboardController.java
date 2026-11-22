package pl.climbbetter.reporting.internal.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.reporting.DashboardPersonalBests;
import pl.climbbetter.reporting.DashboardSessionSummary;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import pl.climbbetter.reporting.DashboardSessionsPage;


@RestController 
@RequestMapping ("/api/v1/dashboard")
class DashboardController {
    private final DashboardQuery dashboardQuery;

    DashboardController(DashboardQuery dashboardQuery){
        this.dashboardQuery = dashboardQuery;
    }

    @GetMapping("/sessions")
    List<DashboardSessionSummary> findRecentSessions(){
        return dashboardQuery.findRecentSessions();
    }

    @GetMapping("/sessions/page")
    DashboardSessionsPage findSessionsPage(@RequestParam(defaultValue = "20") int limit,
                                         @RequestParam(required = false) String cursor) {
        return dashboardQuery.findSessionsPage(limit, cursor);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidPagination(IllegalArgumentException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setProperty("code", "INVALID_PAGINATION");
        return problem;
    }

    @GetMapping("/personal-bests")
    DashboardPersonalBests findPersonalBests() {
        return dashboardQuery.findPersonalBests();
    }
    
}
