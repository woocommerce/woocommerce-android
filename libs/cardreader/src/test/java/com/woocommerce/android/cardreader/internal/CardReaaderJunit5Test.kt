package com.woocommerce.android.cardreader.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MyFirstJUnitJupiterTests {
    private val calculator: Calculator = Calculator()

    @Test
    fun addition() {
        assertEquals(2, calculator.add(1, 1))
    }
}

class Calculator {
    fun add(n: Int, z: Int) = n + z
}
