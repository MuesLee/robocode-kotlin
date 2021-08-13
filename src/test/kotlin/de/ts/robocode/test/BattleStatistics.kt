package de.ts.robocode.test

import robocode.control.snapshot.IBulletSnapshot

class BattleStatistics {

    val shotsFired: MutableMap<Int, IBulletSnapshot> = HashMap()
    val shotsHit: MutableMap<Int, IBulletSnapshot> = HashMap()

    var wallHitCount: Int = 0

    /**
     * This is the accuracy of ALL bullets fired. Keep in mind that there might be bullets that would hit,
     * but do not reach the target before it dies from another bullet.
     */
    fun accuracy(): Double = if (shotsHit.isEmpty()) 0.0 else (shotsHit.size.toDouble() / shotsFired.size.toDouble()) * 100
}
