package pl.climbbetter.catalog.internal;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.catalog.CreateClimbCommand;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController {

    private final CatalogQueryService queryService;
    private final CatalogWriteService writeService;

    CatalogController(CatalogQueryService queryService, CatalogWriteService writeService) {
        this.queryService = queryService;
        this.writeService = writeService;
    }

    @GetMapping("/areas")
    List<CatalogQueryService.AreaOption> areas(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return queryService.findAreas(q, limit);
    }

    @GetMapping("/areas/{areaId}/climbs")
    List<CatalogQueryService.ClimbOption> climbs(
            @PathVariable UUID areaId,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String climbType,
            @RequestParam(required = false) UUID sectorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return queryService.findClimbs(areaId, q, climbType, limit, sectorId);
    }

    @GetMapping("/areas/{areaId}/sectors")
    List<CatalogQueryService.SectorOption> sectors(@PathVariable UUID areaId) {
        return queryService.findSectors(areaId);
    }

    @PostMapping("/areas/{areaId}/climbs")
    @Operation(summary = "Save a private climb without recording a training session")
    ResponseEntity<CatalogQueryService.ClimbOption> createClimb(
            @PathVariable UUID areaId,
            @Valid @RequestBody CreateClimbCommand command
    ) {
        var climb = writeService.create(areaId, command);
        return ResponseEntity.created(URI.create(
                "/api/v1/catalog/areas/" + areaId + "/climbs/" + climb.id()
        )).body(climb);
    }

    @GetMapping("/areas/{areaId}/climbs/{climbId}")
    CatalogQueryService.ClimbOption climb(
            @PathVariable UUID areaId,
            @PathVariable UUID climbId
    ) {
        return queryService.findClimb(areaId, climbId);
    }

    @GetMapping("/grades")
    List<CatalogQueryService.GradeOption> grades(@RequestParam String climbType) {
        return queryService.findGrades(climbType);
    }

    @GetMapping("/effort-profiles")
    List<CatalogQueryService.EffortProfileOption> effortProfiles() {
        return queryService.findEffortProfiles();
    }
}
