package com.woocommerce.android.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class AddressUtilsTest {

    private lateinit var originalDefault: Locale

    @Before
    fun setUp() {
        // Use a stable default locale so display names are deterministic in tests
        originalDefault = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `when blank input, then returns empty string`() {
        assertEquals("", AddressUtils.getCountryLabelByCountryCode(""))
        assertEquals("", AddressUtils.getCountryLabelByCountryCode("   "))
    }

    @Test
    fun `when 2-letter uppercase code, then resolves to display name`() {
        val expected = Locale.Builder().setRegion("US").build().getDisplayCountry(Locale.getDefault())
        val result = AddressUtils.getCountryLabelByCountryCode("US")
        assertEquals(expected, result)
    }

    @Test
    fun `when 2-letter lowercase code, then resolves to display name`() {
        val expected = Locale.Builder().setRegion("GB").build().getDisplayCountry(Locale.getDefault())
        val result = AddressUtils.getCountryLabelByCountryCode("gb")
        assertEquals(expected, result)
    }

    @Test
    fun `when full country name, then returns the same name`() {
        val result = AddressUtils.getCountryLabelByCountryCode("India")
        // In US locale, display name for IN is "India"
        assertEquals("India", result)
    }

    @Test
    fun `when ill-formed 2-letter code, then returns "Unknown Region" value`() {
        val result = AddressUtils.getCountryLabelByCountryCode("ZZ")
        assertEquals("Unknown Region", result)
    }

    @Test
    fun `when non-2-letter arbitrary input, then it is returned as-is`() {
        val input = "Neverland"
        val result = AddressUtils.getCountryLabelByCountryCode(input)
        // There is no ISO country with display name "Neverland"; should echo input
        assertEquals(input, result)
    }
}
