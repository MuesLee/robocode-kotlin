package de.ts.robocode.robots

import robocode.*
import robocode.util.Utils
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.collections.HashMap
import kotlin.math.*


class KotoRobo : AdvancedRobot() {

    companion object {
        private const val MINIMUM_MOVEMENT_DISTANCE: Int = 15
        private const val HALF_ROBOT_SIZE: Double = 18.0

        const val LINEAR_TARGETING = 1
        const val HEAD_ON_TARGETING = 0
        val ALL_TARGETING_STRATEGIES = listOf(HEAD_ON_TARGETING, LINEAR_TARGETING)

        @JvmStatic
        val TARGETING_STATISTICS: MutableMap<String, MutableMap<Int, TargetingStatistics>> = HashMap()
    }

    private val enemies: MutableMap<String, Enemy> = HashMap()

    private val bulletTracker: MutableMap<Bullet, ShotAttempt> = HashMap()

    private var target: Enemy? = null
    private var nextDestination: Point2D.Double = Point2D.Double()

    private var moveDirection = 1

    private fun init() {
        setColors(Color.YELLOW, Color.red, Color.orange)
        isAdjustGunForRobotTurn = true
        isAdjustRadarForGunTurn = true

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY)
        nextDestination = currentPosition()

        enemies.clear()
        bulletTracker.clear()
    }

    override fun run() {
        init()

        do {
            adjustRadar()
            if (hasTarget() && knowsEveryLivingRobot()) {
                attackTarget(target!!)
            }
            execute()
        } while (true)
    }

    override fun onScannedRobot(e: ScannedRobotEvent) {
        val scannedEnemy = Enemy(
            name = e.name,
            pos = currentPosition().extrapolate(e.distance, headingRadians + e.bearingRadians),
            bearing = e.bearing,
            bearingRadians = e.bearingRadians,
            heading = e.heading,
            headingRadians = e.headingRadians,
            velocity = e.velocity,
            distance = e.distance,
            timeOfScan = e.time,
            energy = e.energy
        )

        enemies[e.name] = scannedEnemy

        if (scannedEnemy.name == target?.name) {
            target = scannedEnemy
        }

        if (target == null || e.distance < target!!.distance) {
            target = scannedEnemy
        }
    }

    override fun onRobotDeath(e: RobotDeathEvent) {
        if (target?.name == e.name) {
            target = null
        }

        enemies.remove(e.name)
    }

    override fun onBulletHit(event: BulletHitEvent) {
        val hitShotAttempt = bulletTracker.remove(event.bullet) ?: return
        if (event.name != hitShotAttempt.targetName) return

        TARGETING_STATISTICS[hitShotAttempt.targetName]!![hitShotAttempt.targetingType]!!.shotsHit++
    }

    override fun onBulletMissed(event: BulletMissedEvent) {
        bulletTracker.remove(event.bullet)
    }

    override fun onBulletHitBullet(event: BulletHitBulletEvent) {
        bulletTracker.remove(event.bullet)
    }

    private fun attackTarget(target: Enemy) {
        shootTarget(target)

        val distanceToNextDestination = currentPosition().distance(nextDestination)

        if (distanceToNextDestination < MINIMUM_MOVEMENT_DISTANCE) {
            val battleField = Rectangle2D.Double(
                HALF_ROBOT_SIZE,
                HALF_ROBOT_SIZE,
                battleFieldWidth - HALF_ROBOT_SIZE * 2,
                battleFieldHeight - HALF_ROBOT_SIZE * 2
            )

           findNextDestination(target, battleField)
        }
        moveDirection = 1

        var angle = nextDestination.angleTo(currentPosition()) - headingRadians
        if (cos(angle) < 0) {
            angle += Math.PI
            moveDirection = -1
        }

        setAhead(distanceToNextDestination * moveDirection)
        setTurnRightRadians(Utils.normalRelativeAngle(angle).also { angle = it })
    }

    private fun findNextDestination(
        target: Enemy,
        battleField: Rectangle2D.Double
    ) {
        var testPoint: Point2D.Double?
        var i = 0
        do {
            val distance = (target.distance * 0.8).coerceAtMost(MINIMUM_MOVEMENT_DISTANCE + 200 * Math.random())
            val angle = Math.PI * (1 + 3 * Math.random())
            testPoint = currentPosition().extrapolate(distance, angle)

            if (battleField.contains(testPoint) && ratePoint(testPoint) < ratePoint(nextDestination)) {
                nextDestination = testPoint
            }
        } while (i++ < 200)
    }

    private fun adjustRadar() {
        var radarOffset = 360.0

        if (knowsEveryLivingRobot() && hasTarget() && hasCurrentInformationAboutTarget()) {
            radarOffset = radarHeadingRadians - absbearing(currentPosition(), target!!.pos)

            if (radarOffset < 0) radarOffset -= PI / 8 else radarOffset += PI / 8
        }
        setTurnRadarLeftRadians(normaliseBearing(radarOffset))
    }

    private fun hasCurrentInformationAboutTarget() = if (target == null) false else time - target!!.timeOfScan < 4

    private fun knowsEveryLivingRobot() = others == enemies.size

    private fun shootTarget(target: Enemy) {
        when (chooseBestTargetingStrategy(target)) {
            HEAD_ON_TARGETING -> headOnTargeting(target)
            LINEAR_TARGETING -> linearTargeting(target)
            else -> headOnTargeting(target)
        }
    }

    fun chooseBestTargetingStrategy(target: Enemy): Int {
        val statsForThisTarget = TARGETING_STATISTICS[target.name] ?: return HEAD_ON_TARGETING

        var bestStrategy: Int? = null

        for (strategy in ALL_TARGETING_STRATEGIES) {
            if (strategy !in statsForThisTarget) {
                bestStrategy = strategy
                break
            }

            if (strategy in statsForThisTarget &&
                (statsForThisTarget[strategy]!!.shotsFired < 10 || statsForThisTarget[strategy]!!.accuracy() >= 75)
            ) {
                bestStrategy = strategy
                break
            }
        }

        if (bestStrategy == null) bestStrategy = statsForThisTarget.values.maxOrNull()!!.targetingType

        println("${target.name} HEAD ON ACC ${statsForThisTarget[HEAD_ON_TARGETING]?.accuracy()} LINEAR ACC ${statsForThisTarget[LINEAR_TARGETING]?.accuracy()}")
        println("best strat is $bestStrategy")

        return bestStrategy
    }

    private fun headOnTargeting(target: Enemy) {
        setTurnGunRightRadians(Utils.normalRelativeAngle(target.pos.angleTo(currentPosition()) - gunHeadingRadians))

        if (canFireBullet()) {
            val firedBullet = setFireBullet(computeFirePower(target))
            trackBullet(firedBullet, target, HEAD_ON_TARGETING)
        }
    }

    private fun linearTargeting(target: Enemy) {

        val firePower = computeFirePower(target)

        val estimatedTimeToHit = time + (target.distance / (20 - (3 * firePower))).toInt()

        val guessX = target.guessX(estimatedTimeToHit, battleFieldWidth)
        val guessY = target.guessY(estimatedTimeToHit, battleFieldHeight)

        val estimatedTargetPosition =
            Point2D.Double(guessX, guessY)

        val gunOffset = gunHeadingRadians - absbearing(currentPosition(), estimatedTargetPosition)

        setTurnGunLeftRadians(normaliseBearing(gunOffset))

        if (canFireBullet()) {
            val firedBullet = setFireBullet(firePower)
            trackBullet(firedBullet, target, LINEAR_TARGETING)
        }
    }

    private fun trackBullet(bullet: Bullet, target: Enemy, targetingType: Int) {
        TARGETING_STATISTICS.putIfAbsent(
            target.name,
            mutableMapOf(
                LINEAR_TARGETING to TargetingStatistics(LINEAR_TARGETING),
                HEAD_ON_TARGETING to TargetingStatistics(HEAD_ON_TARGETING)
            )
        )
        TARGETING_STATISTICS[target.name]!![targetingType]!!.shotsFired++
        bulletTracker[bullet] = ShotAttempt(target.name, targetingType)
    }

    private fun canFireBullet() = gunTurnRemaining <= 0.5 && energy > 1 && gunHeat == 0.0

    override fun onPaint(g: Graphics2D) {
        if (!hasTarget()) return
        val targetX = target!!.pos.x.toInt()
        val targetY = target!!.pos.y.toInt()
        g.color = Color.RED
        g.drawLine(targetX, targetY, x.toInt(), y.toInt())
        g.fillRect(targetX - 18, targetY - 18, 36, 36)

        val firePower = computeFirePower(target!!)

        val estimatedTimeToHit = time + (target!!.distance / (20 - (3 * firePower))).toInt()

        val estimatedTargetPosition =
            Point2D.Double(
                target!!.guessX(estimatedTimeToHit, battleFieldWidth),
                target!!.guessY(estimatedTimeToHit, battleFieldHeight)
            )

        val estimatedX = estimatedTargetPosition.x.toInt()
        val estimatedY = estimatedTargetPosition.y.toInt()
        g.color = Color.BLUE
        g.drawLine(estimatedX, estimatedY, x.toInt(), y.toInt())
        g.fillRect(estimatedX - 18, estimatedY - 18, 36, 36)
    }

    private fun computeFirePower(target: Enemy) =
        (energy / 6.0).coerceAtMost(1300.0 / target.distance).coerceAtMost(target.energy / 3.0)

    private fun currentPosition(): Point2D.Double {
        return Point2D.Double(x, y)
    }

    private fun hasTarget() = target != null

    private fun ratePoint(p: Point2D.Double): Double {
        var forceFromCurrentPosition = 1 / p.distanceSq(currentPosition())

        enemies.values.forEach {
            val threatLevelByEnergy = (it.energy / energy).coerceAtMost(2.0)
            val angleRating = 1 + abs(cos(currentPosition().angleTo(p) - it.pos.angleTo(p)))
            val riskForEnemy = threatLevelByEnergy * angleRating / p.distanceSq(it.pos)

            forceFromCurrentPosition += riskForEnemy
        }
        return forceFromCurrentPosition
    }

    private fun Point2D.Double.angleTo(otherPoint2D: Point2D.Double): Double {
        return atan2(this.x - otherPoint2D.x, this.y - otherPoint2D.y)
    }

    private fun Point2D.Double.extrapolate(dist: Double, ang: Double): Point2D.Double {
        return Point2D.Double(this.x + dist * sin(ang), this.y + dist * cos(ang))
    }

    private fun normaliseBearing(ang: Double): Double {
        var normAng = ang
        if (normAng > PI) normAng -= 2 * PI
        if (normAng < -PI) normAng += 2 * PI
        return normAng
    }

    private fun absbearing(p1: Point2D.Double, p2: Point2D.Double): Double {
        val xo = p2.x - p1.x
        val yo = p2.y - p1.y
        val h: Double = p2.distance(p1)
        if (xo > 0 && yo > 0) {
            return asin(xo / h)
        }
        if (xo > 0 && yo < 0) {
            return Math.PI - asin(xo / h)
        }
        if (xo < 0 && yo < 0) {
            return Math.PI + asin(-xo / h)
        }
        return if (xo < 0 && yo > 0) {
            2.0 * Math.PI - asin(-xo / h)
        } else 0.0
    }

    data class Enemy(
        val name: String,
        var pos: Point2D.Double,
        var bearing: Double,
        var bearingRadians: Double,
        val heading: Double,
        var headingRadians: Double,
        var velocity: Double,
        var distance: Double,
        var timeOfScan: Long,
        var energy: Double
    ) {
        fun guessX(atTime: Long, battleFieldWidth: Double): Double {
            val diff: Long = atTime - timeOfScan
            return (pos.x + sin(headingRadians) * velocity * diff).coerceAtMost(battleFieldWidth - HALF_ROBOT_SIZE)
                .coerceAtLeast(HALF_ROBOT_SIZE)
        }

        fun guessY(atTime: Long, battleFieldHeight: Double): Double {
            val diff: Long = atTime - timeOfScan
            return (pos.y + cos(headingRadians) * velocity * diff).coerceAtMost(battleFieldHeight - HALF_ROBOT_SIZE)
                .coerceAtLeast(HALF_ROBOT_SIZE)
        }

    }

    private data class ShotAttempt(val targetName: String, val targetingType: Int)

    class TargetingStatistics(
        val targetingType: Int,
        var shotsFired: Int = 0,
        var shotsHit: Int = 0
    ) : Comparable<TargetingStatistics> {

        fun accuracy(): Double = if (shotsHit == 0) 0.00 else (shotsHit.toDouble() / shotsFired.toDouble()) * 100

        override fun compareTo(other: TargetingStatistics): Int {
            return compareValues(this.accuracy(), other.accuracy())
        }

        override fun toString(): String {
            return "Type $targetingType, fired: $shotsFired, hit: $shotsHit, accuracy: ${accuracy()}"
        }
    }
}
