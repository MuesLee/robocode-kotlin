package de.ts.robocode.test

import robocode.control.snapshot.IBulletSnapshot

class BattleStatistics {

    val shotsFired: MutableMap<Int, IBulletSnapshot> = HashMap()
    val shotsHit: MutableMap<Int, IBulletSnapshot> = HashMap()

    var wallHitCount: Int = 0

    fun accuracy() = if (shotsHit.isEmpty()) 0 else (shotsFired.size / shotsHit.size) * 100
}
