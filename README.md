# robocode-kotlin

This project provides an implementation of a [Robocode](https://robocode.sourceforge.io/) robot in Kotlin
and some tooling around it. 

# Get Started

Edit the `gradle.properties` and change the `robocodeHome` path to your local `Robocode` directory.

You also need to change the path to your `Robocode` directory in the `KotoRoboTest.kt` file.

To see the robot in action simply run: 
```
./gradlew startRobocode
```

## Build

This project uses `gradle` for the dependencies and provides some extra tasks. 
Most of the paths and stuff are configurable in the `gradle.properties` 

### roboJar

Builds a `Robocode` compatible `jar` file which contains the `KotoRobo` class and the `KotoRobo.properties` file.

Does not compute the codesize though.

### registerRobo

Copies the built `jar` file into the specified `Robocode` directory.

### generateBattleFile

Uses the template `\config\battles\testBattle.battle`, replaces the `selectedRobots` with the configured robots and
copies the file into the `Robocode` directory.

### cleanUp

This task is executed after each gradle `clean` and deletes the generated robot `jar` and `battle` files from the `Robocode` directory.  

### startRobocode

Executes every task mentioned above and starts the real `Robocode` battle using the generated `testBattle.battle`. 
Lay back and watch you robot fight.

## Test

The `RobocodeTestEngine.kt` class configures the `Robocode` engine for usage in automated tests.
It registers the robots under development and provides a simple way to start a battle and evaluate the `TestBattleResult.kt`

Example:
```kotlin
val testEngine = RobocodeTestEngine("YOUR_ROBOCODE_HOME")

val result = testEngine.startBattle("MyRobotUnderTest", "sample.SittingDuck")

assertThat(result.winnerName).isEqualTo("MyRobotUnderTest")
assertThat(result.roboUnderTestStatistics.accuracy()).isEqualTo(100)
assertThat(result.roboUnderTestResult.firsts).isEqualTo(result.battleSpec.numRounds).`as` { "roboUnderTest should win every round" }

```

## Robot

### Targeting

The robot can use HEAD-ON and LINEAR targeting and chooses the most successful one for each target. 

It tracks every of its bullets and stores the accuracy data between rounds

### Movement

It moves in a combination of [risk-based](https://robowiki.net/wiki/Minimum_Risk_Movement) and [anti-gravity](https://robowiki.net/wiki/Anti-Gravity_Movement) movement,
where it always tries to move away from its current position while keeping a 90° angle to opponents.

### Energy management

Not much so far. It just stops firing if its energy is 1 or less.



# Credits

This project is inspired by these awesome robocode projects:
* [robocode-gradle-plugin ](https://github.com/bnorm/robocode-gradle-plugin)
* [robocode-pmj-dacruzer](https://github.com/philipmjohnson/robocode-pmj-dacruzer)








