package org.wordpress.android.fluxc.persistence.converters

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SemicolonAtSeparatedStringListConverterTest {
    private lateinit var converter: SemicolonAtSeparatedStringListConverter

    @Before
    fun setUp() {
        converter = SemicolonAtSeparatedStringListConverter()
    }

    @Test
    fun `when converting list to string and back, then original list is preserved`() {
        val originalList = listOf("alpha-111", "alpha-112", "beta-113")

        val stringRepresentation = converter.fromStringList(originalList)
        val restoredList = converter.toStringList(stringRepresentation)

        assertEquals(originalList, restoredList)
    }

    @Test
    fun `when converting empty list to string and back, then empty list is preserved`() {
        val emptyList = emptyList<String>()

        val stringRepresentation = converter.fromStringList(emptyList)
        val restoredList = converter.toStringList(stringRepresentation)

        assertEquals(emptyList, restoredList)
    }

    @Test
    fun `when converting null to string and back, then null is preserved`() {
        val stringRepresentation = converter.fromStringList(null)
        val restoredList = converter.toStringList(stringRepresentation)

        assertEquals(null, restoredList)
    }
}
