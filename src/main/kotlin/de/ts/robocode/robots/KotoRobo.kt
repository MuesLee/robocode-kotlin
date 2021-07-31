package de.ts.robocode.robots

import robocode.BulletHitEvent
import robocode.Robot
import robocode.ScannedRobotEvent
import robocode.StatusEvent
import java.awt.Color

open class KotoRobo : Robot() {

    override fun run() {
        super.run()
        setAllColors(Color.ORANGE)

        while (true) {
            turnGunLeft(10.0)
        }
    }

    override fun onBulletHit(event: BulletHitEvent) {
        setScanColor(Color.ORANGE)
    }

    override fun onScannedRobot(event: ScannedRobotEvent) {
        fire(3.0)
    }

    override fun onStatus(e: StatusEvent) {
        super.onStatus(e)

    }
}