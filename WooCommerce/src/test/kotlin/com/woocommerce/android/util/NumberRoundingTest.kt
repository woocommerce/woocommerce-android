package com.woocommerce.android.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@Suppress("ArrayPrimitive")
class NumberRoundingTest(
    private val initialValue: Float,
    private val roundedValue: Float
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Float>> {
            return listOf(
                arrayOf(0f, 0f),
                arrayOf(23f, 30f),
                arrayOf(656f, 700f),
                arrayOf(1526f, 2000f),
                arrayOf(-18f, -20f),
                arrayOf(-138f, -200f),
                arrayOf(-1338f, -2000f),
                arrayOf(0.3f, 0.3f),
                arrayOf(-0.3f, -0.3f),
                arrayOf(1.3f, 2f),
                arrayOf(-1.3f, -2f)
            )
        }
    }

    @Test
    fun `Given float value, when roundToTheNextPowerOfTen, then return the next power of 10 for the value`() {
        assertEquals(roundedValue, initialValue.roundToTheNextPowerOfTen())
    }
}
