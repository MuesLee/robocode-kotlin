package de.ts.robocode.test

import robocode.BattleResults
import robocode.control.BattleSpecification

data class TestBattleResult(
    val winnerName: String,
    val roboUnderTestResult: BattleResults,
    val roboUnderTestStatistics: BattleStatistics,
    val battleSpec: BattleSpecification
) {
}