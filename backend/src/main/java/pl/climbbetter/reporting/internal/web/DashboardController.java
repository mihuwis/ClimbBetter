package pl.climbbetter.reporting.internal.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.reporting.DashboardSessionSummary;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;


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
    
}
