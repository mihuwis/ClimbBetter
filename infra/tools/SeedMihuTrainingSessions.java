import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import pl.climbbetter.ClimbbetterBackendApplication;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.training.CreateTrainingSessionCommand;
import pl.climbbetter.training.UpdateTrainingSessionCommand;
import pl.climbbetter.training.internal.TrainingSessionService;
import pl.climbbetter.training.internal.TrainingSessionDetailsService;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Explicit local demo import. Run from backend using the project's classpath. */
class SeedMihuTrainingSessions {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2
                || (args.length == 2 && !"--update-existing".equals(args[1]))) {
            throw new IllegalArgumentException("Expected JSON path and optional --update-existing");
        }
        var application = new SpringApplication(ClimbbetterBackendApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (var context = application.run(
                "--spring.profiles.active=local",
                "--spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/climbbetter",
                "--spring.datasource.username=climbbetter",
                "--spring.datasource.password=climbbetter-local",
                "--climbbetter.local.user-id=00000000-0000-0000-0000-000000000002",
                "--spring.main.banner-mode=off",
                "--debug=false",
                "--logging.level.root=WARN"
        )) {
            UUID mihu = UUID.fromString("00000000-0000-0000-0000-000000000002");
            if (!mihu.equals(context.getBean(CurrentUserProvider.class).currentUserId())) {
                throw new IllegalStateException("Demo import must run as Mihu");
            }
            var mapper = context.getBean(ObjectMapper.class);
            var commands = mapper.readValue(Files.readString(Path.of(args[0])),
                    CreateTrainingSessionCommand[].class);
            var service = context.getBean(TrainingSessionService.class);
            var detailsService = context.getBean(TrainingSessionDetailsService.class);
            var jdbc = context.getBean(JdbcClient.class);
            var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            List<UUID> ids = transaction.execute(status -> {
                List<UUID> result = new ArrayList<>();
                for (var command : commands) {
                    if (command.sessionName() == null || !command.sessionName().startsWith("[DEMO]")) {
                        throw new IllegalArgumentException("Only explicit [DEMO] sessions are allowed");
                    }
                    UUID existingId = jdbc.sql("""
                                    SELECT id FROM training.training_sessions
                                    WHERE user_id = :userId AND client_session_id = :clientSessionId
                                      AND deleted_at IS NULL
                                    """)
                            .param("userId", mihu).param("clientSessionId", command.clientSessionId())
                            .query(UUID.class).optional().orElse(null);
                    if (existingId != null && args.length == 2) {
                        var before = detailsService.findById(existingId).orElseThrow();
                        if (!before.sessionName().startsWith("[DEMO]")) {
                            throw new IllegalStateException("Existing session is not demo data");
                        }
                        service.update(existingId, new UpdateTrainingSessionCommand(
                                before.version(), "Expand demo history to two sessions per week", command
                        ));
                        result.add(existingId);
                    } else {
                        result.add(service.create(command));
                    }
                }
                return result;
            });
            System.out.println("Committed demo sessions for Mihu: " + ids);
        }
    }
}
