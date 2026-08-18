package pl.climbbetter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.Clock;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ClimbbetterBackendApplicationTests {

	@Autowired
	Clock clock;

	@Test
	void contextLoads() {
	}

	@Test
	void clockUsesUtcZone(){
		assertEquals(ZoneOffset.UTC, clock.getZone());
	}

}
