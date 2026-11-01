package pl.climbbetter.training.internal.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.TrainingSessionDetails;
import pl.climbbetter.training.TrainingSessionSummary;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import pl.climbbetter.training.UpdateTrainingSessionCommand;
import pl.climbbetter.training.internal.TrainingSessionDetailsService;
import pl.climbbetter.training.internal.TrainingSessionService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training")
class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;
    private final TrainingSessionDetailsService detailsService;

    TrainingSessionController(
            TrainingSessionService trainingSessionService,
            TrainingSessionDetailsService detailsService
    ) {
        this.trainingSessionService = trainingSessionService;
        this.detailsService = detailsService;
    }

    @GetMapping("/sessions")
    List<TrainingSessionSummary> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return detailsService.findByDateRange(from, to);
    }

    @PostMapping("/sessions")
    ResponseEntity<CreatedTrainingSessionResponse> create(
            @Valid @RequestBody
            CreateTrainingSessionCommand command
    ) {
        UUID sessionId = trainingSessionService.create(command);

        URI location = URI.create(
                "/api/v1/training/sessions/" + sessionId
        );

        return ResponseEntity
                .created(location)
                .body(new CreatedTrainingSessionResponse(
                        sessionId
                ));
    }

    @PutMapping("/sessions/{sessionId}")
    ResponseEntity<TrainingSessionDetails> update(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateTrainingSessionCommand command
    ) {
        return ResponseEntity.ok(trainingSessionService.update(sessionId, command));
    }

    @DeleteMapping("/sessions/{sessionId}")
    ResponseEntity<Void> delete(
            @PathVariable UUID sessionId,
            @RequestParam long expectedVersion
    ) {
        trainingSessionService.delete(sessionId, expectedVersion);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}")
    ResponseEntity<TrainingSessionDetails> findById(
            @PathVariable UUID sessionId
    ) {
        return detailsService.findById(sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    record CreatedTrainingSessionResponse(
            UUID sessionId
    ) {
    }
}
