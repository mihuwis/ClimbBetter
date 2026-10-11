package pl.climbbetter.training.internal.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.internal.TrainingSessionService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training")
class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    TrainingSessionController(
            TrainingSessionService trainingSessionService
    ) {
        this.trainingSessionService = trainingSessionService;
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

    record CreatedTrainingSessionResponse(
            UUID sessionId
    ) {
    }
}