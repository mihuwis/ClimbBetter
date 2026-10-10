package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntryClassificationTest {

    @ParameterizedTest
    @MethodSource("validClassifications")
    void acceptsValidClassification(
            ClimbType climbType,
            ResultType resultType,
            AscentMode ascentMode,
            ProtectionMode protectionMode
    ) {
        assertDoesNotThrow(() -> new EntryClassification(
                climbType,
                resultType,
                ascentMode,
                protectionMode
        ));
    }

        static Stream<Arguments> validClassifications() {
        return Stream.of(
                Arguments.of(
                        ClimbType.BOULDER,
                        ResultType.ASCENT,
                        AscentMode.OS,
                        null
                ),
                Arguments.of(
                        ClimbType.BOULDER,
                        ResultType.ATTEMPT,
                        AscentMode.RP,
                        null
                ),
                Arguments.of(
                        ClimbType.ROUTE,
                        ResultType.ASCENT,
                        AscentMode.FLASH,
                        ProtectionMode.LEAD
                ),
                Arguments.of(
                        ClimbType.ROUTE,
                        ResultType.ATTEMPT,
                        AscentMode.RP,
                        ProtectionMode.TOP_ROPE
                )
        );
    }

        @Test
        void rejectsWarmUpClassification() {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new EntryClassification(
                                ClimbType.BOULDER,
                                ResultType.WARMUP,
                                null,
                                null
                        )
                );
        }

    @Test
    void rejectsAscentWithoutAscentMode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EntryClassification(
                        ClimbType.BOULDER,
                        ResultType.ASCENT,
                        null,
                        null
                )
        );
    }

    @Test
    void rejectsRouteWithoutProtectionMode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EntryClassification(
                        ClimbType.ROUTE,
                        ResultType.ASCENT,
                        AscentMode.RP,
                        null
                )
        );
    }

    @Test
    void rejectsProtectionModeOutsideRoute() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EntryClassification(
                        ClimbType.BOULDER,
                        ResultType.ASCENT,
                        AscentMode.RP,
                        ProtectionMode.TOP_ROPE
                )
        );
    }

}
