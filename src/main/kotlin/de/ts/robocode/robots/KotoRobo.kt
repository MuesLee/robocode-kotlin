package de.ts.robocode.robots

import robocode.BulletHitEvent
import robocode.Robot
import robocode.ScannedRobotEvent
import robocode.StatusEvent
import java.awt.Color

open class KotoRobo : Robot() {

    override fun run() {
        setBodyColor(Color.GRAY)

        while (true) {
            turnGunLeft(10.0)
        }
    }

    override fun onBulletHit(event: BulletHitEvent) {
    }

    override fun onScannedRobot(event: ScannedRobotEvent) {
        fire(3.0)
        turnGunLeft(0.0)
    }

    override fun onStatus(e: StatusEvent) {
        super.onStatus(e)

    }
}