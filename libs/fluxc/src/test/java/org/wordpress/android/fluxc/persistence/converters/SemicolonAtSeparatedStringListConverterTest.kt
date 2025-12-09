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
    fun `round trip conversion preserves data`() {
        // Given
        val originalList = listOf("alpha-111", "alpha-112", "beta-113")

        // When
        val stringRepresentation = converter.fromStringList(originalList)
        val restoredList = converter.toStringList(stringRepresentation)

        // Then
        assertEquals(originalList, restoredList)
    }
}
