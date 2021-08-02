package de.ts.robocode.test

class BattleStatistics {

    var shotsFired: Int = 0
    var shotsHit: Int = 0

    val accuracy = if (shotsHit == 0) 0 else (shotsFired / shotsHit) * 100
}
