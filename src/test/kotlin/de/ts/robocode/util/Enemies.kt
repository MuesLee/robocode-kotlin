package de.ts.robocode.util

import de.ts.robocode.robots.KotoRobo
import java.awt.geom.Point2D

object Enemies {

    val DEFAULT = KotoRobo.Enemy(
        name = "testTarget",
        pos = Point2D.Double(),
        timeOfScan = 0,
        energy = 100.0,
        bearing = 0.0,
        bearingRadians = 0.0,
        distance = 100.0,
        heading = 0.0,
        headingRadians = 0.0,
        velocity = 0.0)
}