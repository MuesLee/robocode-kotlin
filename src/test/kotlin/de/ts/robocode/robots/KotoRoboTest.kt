package de.ts.robocode.robots

import de.ts.robocode.test.RobocodeTestEngine
import de.ts.robocode.util.Robots
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class KotoRoboTest {

   private val testEngine = RobocodeTestEngine("D:/Programme/Robocode")

    private val roboUnderTestName = "${KotoRobo::class.java.name}*"


    @Test
    fun `robot can be loaded`() {
        val loadRobot = testEngine.loadRobot(roboUnderTestName)

        Assertions.assertThat(loadRobot.name).isEqualTo(roboUnderTestName)
    }

    @Test
    fun `should win versus sittingDuck`() {

        val testBattleResult = testEngine.startBattle(roboUnderTestName, Robots.SittingDuck)

        Assertions.assertThat(testBattleResult.winnerName).isEqualTo(roboUnderTestName)
    }
}