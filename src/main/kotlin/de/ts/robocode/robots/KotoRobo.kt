package de.ts.robocode.robots

import robocode.*
import robocode.util.Utils
import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.collections.HashMap
import kotlin.math.*

class KotoRobo : AdvancedRobot() {

    companion object {
        private const val MINIMUM_MOVEMENT_DISTANCE: Int = 15
    }

    private var enemies: HashMap<String, Enemy> = HashMap()

    private var target: Enemy? = null
    private var nextDestination: Point2D.Double = Point2D.Double()
    private var lastPosition: Point2D.Double = Point2D.Double()
    private var currentPosition: Point2D.Double = Point2D.Double()

    override fun run() {
        init()

        do {
            currentPosition = Point2D.Double(x, y)

            if (hasTarget() && others <= enemies.size) {
                attackTarget(target!!)
            }
            execute()
        } while (true)
    }

    private fun init() {
        setColors(Color.YELLOW, Color.red, Color.orange)
        isAdjustGunForRobotTurn = true
        isAdjustRadarForGunTurn = true

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY)
        currentPosition = Point2D.Double(x, y)
        lastPosition = currentPosition
        nextDestination = lastPosition
    }

    private fun attackTarget(target: Enemy) {
        val distanceToTarget = currentPosition.distance(target.pos)

        if (hasTarget() && gunTurnRemaining == 0.0 && energy > 1) {
            setFire((energy / 6.0).coerceAtMost(1300.0 / distanceToTarget).coerceAtMost(target.energy / 3.0))
        }
        setTurnGunRightRadians(Utils.normalRelativeAngle(target.pos.angleTo(currentPosition) - gunHeadingRadians))

        val distanceToNextDestination = currentPosition.distance(nextDestination)

        if (distanceToNextDestination < MINIMUM_MOVEMENT_DISTANCE) {

            val battleField = Rectangle2D.Double(30.0, 30.0, battleFieldWidth - 60, battleFieldHeight - 60)
            var testPoint: Point2D.Double
            var i = 0
            do {
                val distance = (distanceToTarget * 0.8).coerceAtMost(MINIMUM_MOVEMENT_DISTANCE + 100 * Math.random())
                val angle = Math.PI * (1 + 3 * Math.random())
                testPoint = currentPosition.extrapolate(distance, angle)

                if (battleField.contains(testPoint) && ratePoint(testPoint) < ratePoint(nextDestination)) {
                    nextDestination = testPoint
                }
            } while (i++ < 200)
            lastPosition = currentPosition
        } else {

            var angle = nextDestination.angleTo(currentPosition) - headingRadians
            var direction = 1.0
            if (cos(angle) < 0) {
                angle += Math.PI
                direction = -1.0
            }
            setAhead(distanceToNextDestination * direction)
            setTurnRightRadians(Utils.normalRelativeAngle(angle).also { angle = it })
        }
    }

    private fun hasTarget() = target != null

    private fun ratePoint(p: Point2D.Double): Double {
        var forceFromLastPosition = 1 / p.distanceSq(lastPosition)

        enemies.values.forEach {
            val threatLevelByEnergy = (it.energy / energy).coerceAtMost(2.0)
            val angleRating = 1 + abs(cos(currentPosition.angleTo(p) - it.pos.angleTo(p)))
            val forceForEnemy = threatLevelByEnergy * angleRating / p.distanceSq(it.pos)

            forceFromLastPosition += forceForEnemy
        }
        return forceFromLastPosition
    }

    override fun onScannedRobot(e: ScannedRobotEvent) {
        if (others == 1) setTurnRadarLeftRadians(radarTurnRemainingRadians)

        enemies.putIfAbsent(e.name, Enemy(e.name, Point2D.Double()))

        val scannedEnemy = enemies[e.name] as Enemy
        scannedEnemy.energy = e.energy
        scannedEnemy.pos = currentPosition.extrapolate(e.distance, headingRadians + e.bearingRadians)

        if (target == null || e.distance < currentPosition.distance(target!!.pos)) {
            target = scannedEnemy
        }
    }

    override fun onRobotDeath(e: RobotDeathEvent) {
        if (target?.name == e.name) {
            target = null
        }

        enemies.remove(e.name)
    }

    private fun Point2D.Double.angleTo(otherPoint2D: Point2D.Double): Double {
        return atan2(this.x - otherPoint2D.x, this.y - otherPoint2D.y)
    }

    private fun Point2D.Double.extrapolate(dist: Double, ang: Double): Point2D.Double {
        return Point2D.Double(this.x + dist * sin(ang), this.y + dist * cos(ang))
    }

    private data class Enemy(
        val name: String,
        var pos: Point2D.Double,
        var energy: Double = 0.0
    )
}