package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingMetricsFacadeTest {

    private final TrainingMetricsFacade facade = new TrainingMetricsFacade();

    @ParameterizedTest
    @CsvSource(value = {"BOULDER,NONE,2", "ROUTE,LEAD,2", "ROUTE,TOP_ROPE,2", "CIRCUIT,NONE,2",
            "BOULDER,NONE,10", "ROUTE,LEAD,10", "ROUTE,TOP_ROPE,10", "CIRCUIT,NONE,10"}, nullValues = "NONE")
    void rejectsCompletedMovesDifferentFromTheClimbLength(String climbType, String protectionMode, int executedMoves) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> facade.calculateAssessedV2(
                        climbType, "ASCENT", "RP", protectionMode,
                        new BigDecimal("90"), profile("7"),
                        BigDecimal.ONE, BigDecimal.ONE, 8, executedMoves
                ));
        assertEquals("Completed climbs require exactly 8 executed moves; use Attempt for a partial climb",
                error.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"ASCENT,8", "ATTEMPT,2", "ATTEMPT,10"})
    void preservesFullAscentsAndRepeatedOrPartialAttemptMoves(String resultType, int executedMoves) {
        TrainingMetricsFacade.CalculatedEntry entry = calculateV2(
                "BOULDER", resultType, "92.5", profile("10"), 8, executedMoves
        );
        assertEquals(executedMoves, entry.executedMoves());
    }

    @Test
    void calculatesApprovedV2Examples() {
        TrainingMetricsFacade.CalculatedEntry boulder = calculateV2(
                "BOULDER", "ASCENT", "92.5", profile("10"), 4, 4
        );
        TrainingMetricsFacade.CalculatedEntry mediumRoute = calculateV2(
                "ROUTE", "ASCENT", "90", profile("7"), 20, 20
        );
        TrainingMetricsFacade.CalculatedEntry longRoute = calculateV2(
                "ROUTE", "ASCENT", "90", profile("5"), 120, 120
        );

        assertAll(
                () -> assertV2(boulder, "10", "9.2500000000", "37.0000"),
                () -> assertV2(mediumRoute, "7", "6.3000000000", "126.0000"),
                () -> assertV2(longRoute, "5", "4.5000000000", "540.0000"),
                () -> assertEquals(new BigDecimal("92.5000"), boulder.classicLoad()),
                () -> assertEquals(new BigDecimal("90.0000"), mediumRoute.classicLoad()),
                () -> assertEquals(new BigDecimal("90.0000"), longRoute.classicLoad()),
                () -> assertNull(boulder.crux()),
                () -> assertNull(mediumRoute.crux()),
                () -> assertNull(longRoute.crux())
        );
    }

    @Test
    void splitsThirtyExecutedMovesIntoFiveHardAndTwentyFiveEasy() {
        TrainingMetricsFacade.CalculatedEntry result = calculateV2(
                "ROUTE", "ASCENT", "90", cruxProfile(), 30, 30
        );

        assertAll(
                () -> assertV2(result, "7", "8.7083333333", "261.2500"),
                () -> assertEquals(5, result.crux().hardMoveCount()),
                () -> assertEquals(25, result.crux().easyMoveCount()),
                () -> assertEquals(new BigDecimal("9.75"), result.crux().hardMoveIntensity()),
                () -> assertEquals(new BigDecimal("8.5"), result.crux().easyMoveIntensity()),
                () -> assertEquals("6B", result.crux().baseMoveGradeLabel()),
                () -> assertEquals("6C", result.crux().hardMoveGradeLabel()),
                () -> assertEquals("6A", result.crux().easyMoveGradeLabel())
        );
    }

    @Test
    void usesExecutedMovesForPartialAttemptsAndAppliesMultipliersOnce() {
        TrainingMetricsFacade.CalculatedEntry result = facade.calculateAssessedV2(
                "ROUTE",
                "ATTEMPT",
                "RP",
                "LEAD",
                new BigDecimal("90"),
                cruxProfile(),
                new BigDecimal("0.90"),
                new BigDecimal("1.80"),
                50,
                40
        );

        assertAll(
                () -> assertEquals(6, result.crux().hardMoveCount()),
                () -> assertEquals(34, result.crux().easyMoveCount()),
                () -> assertEquals(new BigDecimal("8.6875000000"), result.moveIntensity()),
                () -> assertEquals(new BigDecimal("64.8000"), result.classicLoad()),
                () -> assertEquals(new BigDecimal("562.9500"), result.adjustedLoad())
        );
    }

    @Test
    void includesRepeatedMovesBeyondTheClimbLength() {
        TrainingMetricsFacade.CalculatedEntry result = facade.calculateAssessedV2(
                "ROUTE",
                "ATTEMPT",
                "RP",
                "LEAD",
                new BigDecimal("90"),
                cruxProfile(),
                new BigDecimal("1.30"),
                new BigDecimal("1.80"),
                30,
                80
        );

        assertAll(
                () -> assertEquals(12, result.crux().hardMoveCount()),
                () -> assertEquals(68, result.crux().easyMoveCount()),
                () -> assertEquals(new BigDecimal("312.0000"), result.classicLoad()),
                () -> assertEquals(new BigDecimal("1626.3000"), result.adjustedLoad())
        );
    }

    @Test
    void derivesAdjustedLoadFromWorkBeforeRoundingTheAverage() {
        EffortIntensityProfile intensityProfile = new EffortIntensityProfile(
                new BigDecimal("7"),
                "6B",
                "6C",
                "6A",
                new BigDecimal("1.0001499999"),
                BigDecimal.ONE
        );

        TrainingMetricsFacade.CalculatedEntry result = calculateV2(
                "ROUTE", "ATTEMPT", "90", intensityProfile, 30, 3
        );

        assertAll(
                () -> assertEquals(new BigDecimal("1.0000500000"), result.moveIntensity()),
                () -> assertEquals(new BigDecimal("3.0001"), result.adjustedLoad())
        );
    }

    @Test
    void safelyHandlesZeroExecutedCruxMovesWithoutAnAverageDivision() {
        TrainingMetricsFacade.CalculatedEntry result = calculateV2(
                "ROUTE", "ATTEMPT", "90", cruxProfile(), 50, 0
        );

        assertAll(
                () -> assertEquals(0, result.crux().hardMoveCount()),
                () -> assertEquals(0, result.crux().easyMoveCount()),
                () -> assertEquals(new BigDecimal("0.0000000000"), result.moveIntensity()),
                () -> assertEquals(new BigDecimal("0.0000"), result.classicLoad()),
                () -> assertEquals(new BigDecimal("0.0000"), result.adjustedLoad()),
                () -> assertNull(facade.calculateSession(List.of(result)).averageMoveIntensity())
        );
    }

    @Test
    void roundsTheHardShareUpForASingleExecutedMove() {
        TrainingMetricsFacade.CalculatedEntry result = calculateV2(
                "ROUTE", "ATTEMPT", "90", cruxProfile(), 50, 1
        );

        assertAll(
                () -> assertEquals(1, result.crux().hardMoveCount()),
                () -> assertEquals(0, result.crux().easyMoveCount()),
                () -> assertEquals(new BigDecimal("9.7500000000"), result.moveIntensity()),
                () -> assertEquals(new BigDecimal("9.7500"), result.adjustedLoad())
        );
    }

    @Test
    void preservesWarmUpSemanticsAndWeightsSessionIntensityByAssessedMoves() {
        TrainingMetricsFacade.CalculatedEntry warmUp = facade.calculateWarmUp(30, "v2");
        TrainingMetricsFacade.CalculatedEntry boulder = calculateV2(
                "BOULDER", "ASCENT", "92.5", profile("10"), 4, 4
        );
        TrainingMetricsFacade.CalculatedEntry route = calculateV2(
                "ROUTE", "ASCENT", "90", profile("7"), 20, 20
        );
        TrainingMetricsFacade.CalculatedSession session = facade.calculateSession(
                List.of(warmUp, boulder, route)
        );

        assertAll(
                () -> assertEquals("v2", warmUp.calculationModelVersion()),
                () -> assertEquals("v2", facade.calculateWarmUp(30).calculationModelVersion()),
                () -> assertNull(warmUp.moveIntensity()),
                () -> assertNull(warmUp.profileIntensityIndex()),
                () -> assertNull(warmUp.crux()),
                () -> assertEquals(new BigDecimal("0.0000"), warmUp.adjustedLoad()),
                () -> assertEquals(54, session.totalMoves()),
                () -> assertEquals(new BigDecimal("182.5000"), session.classicLoad()),
                () -> assertEquals(new BigDecimal("163.0000"), session.adjustedLoad()),
                () -> assertEquals(new BigDecimal("6.7916666667"), session.averageMoveIntensity())
        );
    }

    @Test
    void validatesClassificationAndEffortMultipliersAlsoInV2() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> facade.calculateAssessedV2(
                                "ROUTE", "ASCENT", "RP", null,
                                new BigDecimal("90"), profile("7"),
                                BigDecimal.ONE, BigDecimal.ONE, 20, 20
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> facade.calculateAssessedV2(
                                "ROUTE", "ATTEMPT", "RP", "LEAD",
                                new BigDecimal("90"), cruxProfile(),
                                BigDecimal.ONE, new BigDecimal("-1"), 50, 40
                        )
                )
        );
    }

    private TrainingMetricsFacade.CalculatedEntry calculateV2(
            String climbType,
            String resultType,
            String gradePoints,
            EffortIntensityProfile intensityProfile,
            int totalMoves,
            int executedMoves
    ) {
        return facade.calculateAssessedV2(
                climbType,
                resultType,
                "RP",
                "ROUTE".equals(climbType) ? "LEAD" : null,
                new BigDecimal(gradePoints),
                intensityProfile,
                BigDecimal.ONE,
                BigDecimal.ONE,
                totalMoves,
                executedMoves
        );
    }

    private EffortIntensityProfile profile(String index) {
        return new EffortIntensityProfile(
                new BigDecimal(index), null, null, null, null, null
        );
    }

    private EffortIntensityProfile cruxProfile() {
        return new EffortIntensityProfile(
                new BigDecimal("7"),
                "6B",
                "6C",
                "6A",
                new BigDecimal("9.75"),
                new BigDecimal("8.5")
        );
    }

    private void assertV2(
            TrainingMetricsFacade.CalculatedEntry entry,
            String profileIntensityIndex,
            String moveIntensity,
            String adjustedLoad
    ) {
        assertAll(
                () -> assertEquals("v2", entry.calculationModelVersion()),
                () -> assertEquals(new BigDecimal(profileIntensityIndex), entry.profileIntensityIndex()),
                () -> assertEquals(new BigDecimal(moveIntensity), entry.moveIntensity()),
                () -> assertEquals(new BigDecimal(adjustedLoad), entry.adjustedLoad())
        );
    }
}
