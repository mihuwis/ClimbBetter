package pl.climbbetter.training.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.TrainingSessionDetails;
import pl.climbbetter.training.TrainingSessionSummary;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    public List<TrainingSessionSummary> findByDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range must be ordered and no longer than 366 days");
        }
        return repository.findByDateRange(currentUserProvider.currentUserId(), from, to);
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
