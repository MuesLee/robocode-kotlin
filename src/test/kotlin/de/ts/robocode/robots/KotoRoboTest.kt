package de.ts.robocode.robots

import de.ts.robocode.test.RobocodeTestEngine
import de.ts.robocode.util.Robots
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KotoRoboTest {

   private val testEngine = RobocodeTestEngine("D:/Programme/Robocode")

    private val roboUnderTestName = "${KotoRobo::class.java.name}*"


    @Test
    fun `robot can be loaded`() {
        val loadRobot = testEngine.loadRobot(roboUnderTestName)

        Assertions.assertThat(loadRobot.name).isEqualTo(roboUnderTestName)
    }

    @Test
    fun `should hit every bullet versus Sittingduck`() {

        val result = testEngine.startBattle(roboUnderTestName, Robots.SittingDuck)

        Assertions.assertThat(result.roboUnderTestStatistics.accuracy()).isEqualTo(100)
    }

    @ParameterizedTest(name = "should win every round versus {0}")
    @ValueSource(strings = [Robots.SittingDuck, Robots.Corners])
    fun `should win every round versus a dumb opponent`(opponentName: String) {

        val result = testEngine.startBattle(roboUnderTestName, opponentName)

        Assertions.assertThat(result.winnerName).isEqualTo(roboUnderTestName)
        Assertions.assertThat(result.roboUnderTestResult.firsts).isEqualTo(result.battleSpec.numRounds)
    }

    @ParameterizedTest(name = "should win every round versus {0}")
    @ValueSource(strings = [Robots.MyFirstJuniorRobot])
    fun `should win 9 out of 10 rounds versus a decent opponent`(opponentName: String) {

        val result = testEngine.startBattle(roboUnderTestName, opponentName)

        Assertions.assertThat(result.winnerName).isEqualTo(roboUnderTestName)
        Assertions.assertThat(result.roboUnderTestResult.firsts).isGreaterThan(result.battleSpec.numRounds / 100 * 9)
    }

    @Test
    fun `should win at least 8 of 10 rounds versus all dumb opponents at once`() {

        val result = testEngine.startBattle(roboUnderTestName, Robots.ALL_EASY_ROBOTS)

        Assertions.assertThat(result.winnerName).isEqualTo(roboUnderTestName)
        Assertions.assertThat(result.roboUnderTestResult.firsts).isGreaterThan(result.battleSpec.numRounds / 100 * 8)
    }
}