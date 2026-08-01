package com.hdclark.bubbleroulette

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    @Test
    fun everyLevelHasExactlyOneChampagneAndOneNewDecoy() {
        val factory = RoundFactory(Random(20260801))

        for (level in 1..50) {
            val round = factory.create(level)

            assertEquals(level + 1, round.glasses.size)
            assertEquals(1, round.glasses.count { it.kind == DrinkKind.CHAMPAGNE })
            assertEquals(level, round.glasses.count { it.kind == DrinkKind.BUBBLE_MIXTURE })
            assertEquals(round.glasses.size, round.glasses.map(DrinkGlass::id).distinct().size)
        }
    }

    @Test
    fun shufflingPreservesEveryGlassAndItsIdentity() {
        val factory = RoundFactory(Random(17))
        val original = factory.create(level = 12)
        val shuffled = factory.shuffled(original)

        assertEquals(original.level, shuffled.level)
        assertEquals(original.transition, shuffled.transition)
        assertEquals(original.glasses.toSet(), shuffled.glasses.toSet())
        assertEquals(original.glasses.size, shuffled.glasses.size)
    }

    @Test
    fun selectingChampagneWinsAndSelectingDecoyLoses() {
        val round = RoundFactory(Random(9)).create(level = 5)
        val champagne = round.glasses.single { it.kind == DrinkKind.CHAMPAGNE }
        val decoy = round.glasses.first { it.kind == DrinkKind.BUBBLE_MIXTURE }

        assertEquals(GuessResult.CHAMPAGNE, GameRules.guess(round, champagne.id))
        assertEquals(GuessResult.BUBBLE_MIXTURE, GameRules.guess(round, decoy.id))
    }

    @Test
    fun progressionAddsExactlyOneToTheLevel() {
        assertEquals(2, GameRules.nextLevel(1))
        assertEquals(42, GameRules.nextLevel(41))
        assertNotEquals(41, GameRules.nextLevel(41))
    }

    @Test
    fun invalidRoundsAndSelectionsFailFast() {
        assertThrows(IllegalArgumentException::class.java) {
            RoundFactory(Random(1)).create(level = 0)
        }

        val round = RoundFactory(Random(1)).create(level = 2)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            GameRules.guess(round, glassId = 999)
        }
        assertTrue(exception.message.orEmpty().contains("999"))
    }
}
