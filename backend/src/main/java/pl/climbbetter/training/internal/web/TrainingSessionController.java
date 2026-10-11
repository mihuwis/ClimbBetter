package pl.climbbetter.training.internal.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.TrainingSessionDetails;
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
