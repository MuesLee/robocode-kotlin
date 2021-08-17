package de.ts.robocode.robots

import de.ts.robocode.util.Enemies
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import robocode.Bullet
import robocode.BulletHitEvent
import robocode.BulletMissedEvent

class KotoRoboTest {

    @Test
    fun `should track bullets correctly`() {
        val robot = KotoRobo()

        val target = Enemies.DEFAULT

        val name = "robotUnderTest"
        val bulletThatHit = Bullet(0.0, 0.0, 0.0, 0.0, name, Enemies.DEFAULT.name, true, 0)
        val bulletThatMissed = Bullet(0.0, 0.0, 0.0, 0.0, name, Enemies.DEFAULT.name, true, 1)
        val bulletThatMissedAswell = Bullet(0.0, 0.0, 0.0, 0.0, name, Enemies.DEFAULT.name, true, 2)
        val bulletMidAir = Bullet(0.0, 0.0, 0.0, 0.0, name, Enemies.DEFAULT.name, true, 3)
        robot.trackBullet(bulletThatHit, target, KotoRobo.LINEAR_TARGETING)
        robot.trackBullet(bulletThatMissed, target, KotoRobo.LINEAR_TARGETING)
        robot.trackBullet(bulletThatMissedAswell, target, KotoRobo.LINEAR_TARGETING)
        robot.trackBullet(bulletMidAir, target, KotoRobo.LINEAR_TARGETING)

        robot.onBulletHit(BulletHitEvent(Enemies.DEFAULT.name, 5.0, bulletThatHit))
        robot.onBulletMissed(BulletMissedEvent(bulletThatMissed))
        robot.onBulletMissed(BulletMissedEvent(bulletThatMissedAswell))

        Assertions.assertThat(robot.bulletTracker).containsOnlyKeys(bulletMidAir)
        Assertions.assertThat(KotoRobo.TARGETING_STATISTICS[target.name]!![KotoRobo.LINEAR_TARGETING]!!.shotsFired).isEqualTo(4)
        Assertions.assertThat(KotoRobo.TARGETING_STATISTICS[target.name]!![KotoRobo.LINEAR_TARGETING]!!.shotsHit).isEqualTo(1)
        Assertions.assertThat(KotoRobo.TARGETING_STATISTICS[target.name]!![KotoRobo.LINEAR_TARGETING]!!.accuracy()).isEqualTo(25.00)
    }


    @Test
    fun `should choose HEAD-ON targeting as first strategy`() {
        val robot = KotoRobo()
        val target = Enemies.DEFAULT

        val actual = robot.chooseBestTargetingStrategy(target, HashMap())

        Assertions.assertThat(actual).isEqualTo(KotoRobo.HEAD_ON_TARGETING)
    }

    @Test
    fun `should keep testing HEAD-ON strategy if it has not been used at least 5 times`() {
        val robot = KotoRobo()
        val target = Enemies.DEFAULT

        val targetingStatistics = mutableMapOf(
            target.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(
                            KotoRobo.HEAD_ON_TARGETING,
                            4,
                            2
                        )
                    )
        )

        val actual = robot.chooseBestTargetingStrategy(target, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(KotoRobo.HEAD_ON_TARGETING)
    }

    @Test
    fun `should choose LINEAR targeting if HEAD-ON does not work and LINEAR has not been tested before`() {
        val robot = KotoRobo()
        val target = Enemies.DEFAULT

        val targetingStatistics = mutableMapOf(
            target.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 2)
                    )
        )

        val actual = robot.chooseBestTargetingStrategy(target, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(KotoRobo.LINEAR_TARGETING)
    }

    @Test
    fun `should keep testing LINEAR targeting if HEAD-ON does not work and LINEAR has not been tested at least 5 times`() {
        val robot = KotoRobo()
        val target = Enemies.DEFAULT

        val targetingStatistics = mutableMapOf(
            target.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 8),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 4, 2)
                    )
        )

        val actual = robot.chooseBestTargetingStrategy(target, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(KotoRobo.LINEAR_TARGETING)
    }

    @Test
    fun `should choose the best targeting strategy if everything has been tested at least 10 times`() {
        val robot = KotoRobo()
        val target = Enemies.DEFAULT

        val targetingStatistics = mutableMapOf(
            target.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 2),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 15, 5)
                    )
        )

        val actual = robot.chooseBestTargetingStrategy(target, targetingStatistics)

        Assertions.assertThat(targetingStatistics[target.name]?.size)
            .isEqualTo(KotoRobo.ALL_TARGETING_STRATEGIES.size)
            .`as` { "This test should provide data for all available strategies" }
        Assertions.assertThat(actual).isEqualTo(KotoRobo.LINEAR_TARGETING)
    }


    @Test
    fun `should choose the nearest target if there is no targeting data`() {
        val robot = KotoRobo()
        val targetNear = Enemies.DEFAULT.copy(name = "Near", distance = 500.0)
        val targetFarAway = Enemies.DEFAULT.copy(name = "Far Away", distance = 1000.0)

        val enemies = mutableMapOf(targetNear.name to targetNear, targetFarAway.name to targetFarAway)
        val actual = robot.chooseTarget(enemies, HashMap())

        Assertions.assertThat(actual).isEqualTo(targetNear)
    }

    @Test
    fun `should choose the nearest target if it is super near`() {
        val robot = KotoRobo()
        val targetSuperNear = Enemies.DEFAULT.copy(name = "Near", distance = 20.0)
        val targetFarAway = Enemies.DEFAULT.copy(name = "Far Away", distance = 1000.0)

        val targetingStatistics = mutableMapOf(
            targetSuperNear.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 2),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 9, 2)
                    ),
            targetFarAway.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 10),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 9, 2)
                    )
        )


        val enemies = mutableMapOf(targetSuperNear.name to targetSuperNear, targetFarAway.name to targetFarAway)
        val actual = robot.chooseTarget(enemies, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(targetSuperNear)
    }

    @Test
    fun `should return null if there is no known enemy but targeting data from previous rounds`() {
        val robot = KotoRobo()
        val target = Enemies.DEFAULT

        val targetingStatistics = mutableMapOf(
            target.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 2),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 9, 2)
                    )
        )

        val actual = robot.chooseTarget(HashMap(), targetingStatistics)

        Assertions.assertThat(actual).isNull()
    }

    @Test
    fun `should choose the undiscovered target if it has not gathered data about all living robots and no robot is easy to hit yet`() {
        val robot = KotoRobo()

        val targetNearButDodgy = Enemies.DEFAULT.copy(name = "Near", distance = 60.0)
        val targetFarAwayDodgy = Enemies.DEFAULT.copy(name = "Far Away", distance = 500.0)
        val targetAlreadyDead = Enemies.DEFAULT.copy(name = "Dead", distance = 1.0)
        val targetNotShotAtYet = Enemies.DEFAULT.copy(name = "Not shot at yet", distance = 50.0)

        val enemies = mutableMapOf(
            targetNearButDodgy.name to targetNearButDodgy,
            targetFarAwayDodgy.name to targetFarAwayDodgy,
            targetNotShotAtYet.name to targetNotShotAtYet
        )

        val targetingStatistics = mutableMapOf(
            targetAlreadyDead.name to mutableMapOf(
                KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 25, 25),
                KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 12, 12)
            ),
            targetNearButDodgy.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 25, 2),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 12, 2)
                    ),
            targetFarAwayDodgy.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 3),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 0, 0)
                    )
        )

        val actual = robot.chooseTarget(enemies, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(targetNotShotAtYet)
    }

    @Test
    fun `should choose the robot which is good to hit even if it has not gathered data about all living robots`() {
        val robot = KotoRobo()

        val targetNearButDodgy = Enemies.DEFAULT.copy(name = "Near", distance = 60.0)
        val targetFarAwayJustSitting = Enemies.DEFAULT.copy(name = "Far Away", distance = 500.0)
        val targetAlreadyDead = Enemies.DEFAULT.copy(name = "Dead", distance = 1.0)
        val targetNotShotAtYet = Enemies.DEFAULT.copy(name = "Not shot at yet", distance = 80.0)

        val enemies = mutableMapOf(
            targetNearButDodgy.name to targetNearButDodgy,
            targetFarAwayJustSitting.name to targetFarAwayJustSitting,
            targetNotShotAtYet.name to targetNotShotAtYet
        )

        val targetingStatistics = mutableMapOf(
            targetAlreadyDead.name to mutableMapOf(
                KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 25, 25),
                KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 12, 12)
            ),
            targetNearButDodgy.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 25, 2),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 12, 2)
                    ),
            targetFarAwayJustSitting.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 10),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 0, 0)
                    )
        )

        val actual = robot.chooseTarget(enemies, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(targetFarAwayJustSitting)
    }

    @Test
    fun `should choose the target that is the easiest to hit and still alive`() {
        val robot = KotoRobo()

        val targetNearButDodgy = Enemies.DEFAULT.copy(name = "Near", distance = 80.0)
        val targetFarAwayJustSitting = Enemies.DEFAULT.copy(name = "Far Away", distance = 500.0)
        val targetAlreadyDead = Enemies.DEFAULT.copy(name = "Dead", distance = 1.0)

        val enemies = mutableMapOf(
            targetNearButDodgy.name to targetNearButDodgy,
            targetFarAwayJustSitting.name to targetFarAwayJustSitting
        )

        val targetingStatistics = mutableMapOf(
            targetAlreadyDead.name to mutableMapOf(
                KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 25, 25),
                KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 12, 12)
            ),
            targetNearButDodgy.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 25, 2),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 12, 2)
                    ),
            targetFarAwayJustSitting.name to
                    mutableMapOf(
                        KotoRobo.HEAD_ON_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.HEAD_ON_TARGETING, 11, 10),
                        KotoRobo.LINEAR_TARGETING to KotoRobo.TargetingStatistics(KotoRobo.LINEAR_TARGETING, 0, 0)
                    )
        )

        val actual = robot.chooseTarget(enemies, targetingStatistics)

        Assertions.assertThat(actual).isEqualTo(targetFarAwayJustSitting)
    }
}