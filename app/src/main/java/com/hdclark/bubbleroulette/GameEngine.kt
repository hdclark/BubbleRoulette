package com.hdclark.bubbleroulette

import kotlin.random.Random

enum class DrinkKind {
    CHAMPAGNE,
    BUBBLE_MIXTURE,
}

data class DrinkGlass(
    val id: Int,
    val kind: DrinkKind,
)

enum class TransitionStyle {
    VISIBLE_SHUFFLE,
    NADINE_OCCLUSION,
}

data class RoundSpec(
    val level: Int,
    val glasses: List<DrinkGlass>,
    val transition: TransitionStyle,
) {
    init {
        require(level >= 1) { "Level must be at least one." }
        require(glasses.size == level + 1) {
            "Level $level must contain one champagne glass and $level decoys."
        }
        require(glasses.count { it.kind == DrinkKind.CHAMPAGNE } == 1) {
            "Every round must contain exactly one champagne glass."
        }
        require(glasses.map(DrinkGlass::id).distinct().size == glasses.size) {
            "Every glass must have a stable, unique identity."
        }
    }
}

class RoundFactory(
    private val random: Random = Random.Default,
) {
    fun create(level: Int): RoundSpec {
        require(level >= 1) { "Level must be at least one." }

        val glasses = buildList {
            add(DrinkGlass(id = 0, kind = DrinkKind.CHAMPAGNE))
            repeat(level) { decoyIndex ->
                add(
                    DrinkGlass(
                        id = decoyIndex + 1,
                        kind = DrinkKind.BUBBLE_MIXTURE,
                    ),
                )
            }
        }.shuffled(random)

        return RoundSpec(
            level = level,
            glasses = glasses,
            transition = if (random.nextBoolean()) {
                TransitionStyle.VISIBLE_SHUFFLE
            } else {
                TransitionStyle.NADINE_OCCLUSION
            },
        )
    }

    fun shuffled(round: RoundSpec): RoundSpec =
        round.copy(glasses = round.glasses.shuffled(random))
}

enum class GuessResult {
    CHAMPAGNE,
    BUBBLE_MIXTURE,
}

object GameRules {
    fun guess(round: RoundSpec, glassId: Int): GuessResult {
        val selected = round.glasses.firstOrNull { it.id == glassId }
            ?: throw IllegalArgumentException("Glass $glassId is not part of this round.")

        return when (selected.kind) {
            DrinkKind.CHAMPAGNE -> GuessResult.CHAMPAGNE
            DrinkKind.BUBBLE_MIXTURE -> GuessResult.BUBBLE_MIXTURE
        }
    }

    fun nextLevel(currentLevel: Int): Int {
        require(currentLevel >= 1) { "Level must be at least one." }
        return currentLevel + 1
    }
}
