package org.wordpress.android.fluxc.persistence.converters

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.persistence.entity.AppVersionTargets

class AppVersionTargetsConverterTest {
    private lateinit var converter: AppVersionTargetsConverter

    @Before
    fun setUp() {
        converter = AppVersionTargetsConverter()
    }

    @Test
    fun `when converting list to string and back, then original list is preserved`() {
        val originalList = AppVersionTargets(listOf("alpha-111", "alpha-112", "beta-113"))

        val stringRepresentation = converter.fromAppVersionTargets(originalList)
        val restoredList = converter.toAppVersionTargets(stringRepresentation)

        assertEquals(originalList, restoredList)
    }

    @Test
    fun `when converting empty list to string and back, then empty list is preserved`() {
        val emptyList = AppVersionTargets(emptyList())

        val stringRepresentation = converter.fromAppVersionTargets(emptyList)
        val restoredList = converter.toAppVersionTargets(stringRepresentation)

        assertEquals(emptyList, restoredList)
    }

    @Test
    fun `when converting null to string and back, then null is preserved`() {
        val stringRepresentation = converter.fromAppVersionTargets(null)
        val restoredList = converter.toAppVersionTargets(stringRepresentation)

        assertEquals(null, restoredList)
    }

    @Test
    fun `when list contains items with commas, then commas are preserved`() {
        val listWithCommas = AppVersionTargets(listOf("version-1,2", "version-3,4,5"))

        val stringRepresentation = converter.fromAppVersionTargets(listWithCommas)
        val restoredList = converter.toAppVersionTargets(stringRepresentation)

        assertEquals(listWithCommas, restoredList)
    }
}
