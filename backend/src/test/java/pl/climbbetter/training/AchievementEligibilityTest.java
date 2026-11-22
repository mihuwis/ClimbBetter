package pl.climbbetter.training;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementEligibilityTest {
    @ParameterizedTest
    @CsvSource(value = {
            "ASCENT,ROUTE,LEAD,true",
            "ASCENT,ROUTE,TOP_ROPE,false",
            "ASCENT,ROUTE,NULL,false",
            "ASCENT,BOULDER,NULL,true",
            "ASCENT,CIRCUIT,NULL,true",
            "ASCENT,BOULDER,TOP_ROPE,false",
            "ASCENT,UNKNOWN,NULL,false",
            "ATTEMPT,ROUTE,LEAD,false",
            "ATTEMPT,BOULDER,NULL,false",
            "WARMUP,NULL,NULL,false",
            "NULL,ROUTE,LEAD,false"
    }, nullValues = "NULL")
    void distinguishesAchievementsFromTraining(
            String resultType, String climbType, String protectionMode, boolean expected
    ) {
        assertEquals(expected,
                AchievementEligibility.isEligible(resultType, climbType, protectionMode));
    }
}
