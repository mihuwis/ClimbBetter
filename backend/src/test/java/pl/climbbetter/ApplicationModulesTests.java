package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import pl.climbbetter.ClimbbetterBackendApplication;

public class ApplicationModulesTests {
    private final ApplicationModules modules = ApplicationModules.of(ClimbbetterBackendApplication.class);

    @Test
    void verifyModulesStructure() {
        modules.verify();
    }
}
