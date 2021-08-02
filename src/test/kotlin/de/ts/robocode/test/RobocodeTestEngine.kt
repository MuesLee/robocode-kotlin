package de.ts.robocode.test

import net.sf.robocode.settings.SettingsManager
import robocode.control.BattleSpecification
import robocode.control.BattlefieldSpecification
import robocode.control.RobocodeEngine
import robocode.control.RobotSpecification
import robocode.control.events.BattleAdaptor
import robocode.control.events.BattleCompletedEvent
import robocode.control.events.TurnEndedEvent
import java.io.File

class RobocodeTestEngine(robocodeHomePath: String) : BattleAdaptor() {

    private val engine: RobocodeEngine

    private var completedEvent: BattleCompletedEvent? = null

    private var statistics = BattleStatistics()

    init {
        registerLocalDevRobots()

        engine = RobocodeEngine(File(robocodeHomePath))
        engine.addBattleListener(this)
    }

    fun startBattle(roboUnderTestName: String,
                    vararg opponentNames: String = arrayOf("sample.SittingDuck")): TestBattleResult {
        resetState()

        val selectedRobots =engine.getLocalRepository("$roboUnderTestName,${opponentNames.joinToString(separator = ",")}")

        val numberOfRounds = 100
        val battleFieldSpec = BattlefieldSpecification(800, 600)
        val battleSpec = BattleSpecification(numberOfRounds, battleFieldSpec, selectedRobots)

        engine.runBattle(battleSpec, true)

        val winnerResult = completedEvent!!.sortedResults.first()
        val roboUnderTestResult = completedEvent!!.indexedResults.first()

        return TestBattleResult(selectedRobots[winnerResult.rank-1].name, roboUnderTestResult, statistics, battleSpec)
    }

    fun loadRobot(robotName: String): RobotSpecification {
        return engine.getLocalRepository(robotName).first()
    }

    override fun onBattleCompleted(event: BattleCompletedEvent) {
        completedEvent = event
    }

    override fun onTurnEnded(event: TurnEndedEvent) {
        val allBulletsShotByTestRobo = event.turnSnapshot.bullets.filter { it.ownerIndex == 1 }
        val allBulletsThatHit = allBulletsShotByTestRobo.filter { it.victimIndex != -1 }

        statistics.shotsFired += allBulletsShotByTestRobo.count()
        statistics.shotsHit += allBulletsThatHit.count()
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