package de.ts.robocode.test

import net.sf.robocode.settings.SettingsManager
import robocode.control.BattleSpecification
import robocode.control.BattlefieldSpecification
import robocode.control.RobocodeEngine
import robocode.control.RobotSpecification
import robocode.control.events.BattleAdaptor
import robocode.control.events.BattleCompletedEvent
import robocode.control.events.TurnEndedEvent
import robocode.control.snapshot.BulletState
import java.io.File

class RobocodeTestEngine(robocodeHomePath: String) : BattleAdaptor() {

    companion object {
        private const val ROBO_SIZE = 18
    }

    private val engine: RobocodeEngine

    private var completedEvent: BattleCompletedEvent? = null

    private var statistics = BattleStatistics()

    private val battlefieldSpecification: BattlefieldSpecification = BattlefieldSpecification(800, 600)

    init {
        registerLocalDevRobots()

        engine = RobocodeEngine(File(robocodeHomePath))
        engine.addBattleListener(this)
    }

    fun startBattle(
        roboUnderTestName: String,
        vararg opponentNames: String = arrayOf("sample.SittingDuck")
    ): TestBattleResult {
        resetState()

        val selectedRobots =
            engine.getLocalRepository("$roboUnderTestName,${opponentNames.joinToString(separator = ",")}")

        val numberOfRounds = 100
        val battleSpec = BattleSpecification(numberOfRounds, battlefieldSpecification, selectedRobots)

        engine.runBattle(battleSpec, true)

        val winnerResult = completedEvent!!.sortedResults.first()
        val roboUnderTestResult = completedEvent!!.indexedResults.first()

        return TestBattleResult(selectedRobots[winnerResult.rank - 1].name, roboUnderTestResult, statistics, battleSpec)
    }

    fun loadRobot(robotName: String): RobotSpecification {
        return engine.getLocalRepository(robotName).first()
    }

    override fun onBattleCompleted(event: BattleCompletedEvent) {
        completedEvent = event
    }

    override fun onTurnEnded(event: TurnEndedEvent) {
        val turnSnapshot = event.turnSnapshot

        val testRobo = turnSnapshot.robots.first { it.robotIndex == 0 }
        if (testRobo.x <= ROBO_SIZE || testRobo.x >= battlefieldSpecification.height - ROBO_SIZE ||
            testRobo.y <= ROBO_SIZE || testRobo.y >= battlefieldSpecification.height - ROBO_SIZE
        ) {
            statistics.wallHitCount++
        }

        val allBulletsShotByTestRobo = turnSnapshot.bullets.filter { it.ownerIndex == testRobo.contestantIndex }
        val allBulletsThatHit = allBulletsShotByTestRobo.filter { it.state == BulletState.HIT_VICTIM }

        allBulletsShotByTestRobo.forEach { statistics.shotsFired[it.bulletId] = it }
        allBulletsThatHit.forEach { statistics.shotsHit[it.bulletId] = it }
    }

    private fun registerLocalDevRobots() {
        val executionPath = System.getProperty("user.dir")
        System.setProperty(SettingsManager.OPTIONS_DEVELOPMENT_PATH, "$executionPath/build/classes/kotlin/main")
    }

    private fun resetState() {
        completedEvent = null
        statistics = BattleStatistics()
    }
}