package pl.climbbetter.training.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.TrainingSessionDetails;

import java.util.Optional;
import java.util.UUID;

@Service
public class TrainingSessionDetailsService {

    private final CurrentUserProvider currentUserProvider;
    private final TrainingSessionRepository repository;

    TrainingSessionDetailsService(
            CurrentUserProvider currentUserProvider,
            TrainingSessionRepository repository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<TrainingSessionDetails> findById(UUID sessionId) {
        UUID userId = currentUserProvider.currentUserId();

        return repository.findDetailsSession(userId, sessionId)
                .map(session -> session.toDetails(
                        repository.findDetailsEntries(
                                userId,
                                sessionId
                        )
                ));
    }
}
