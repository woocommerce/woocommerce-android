package com.woocommerce.android.util

import org.junit.After
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

    @After
    fun tearDown() {
        Locale.setDefault(originalDefault)
    }

    @Test
    fun `blank input returns empty string`() {
        assertEquals("", AddressUtils.getCountryLabelByCountryCode(""))
        assertEquals("", AddressUtils.getCountryLabelByCountryCode("   "))
    }

    @Test
    fun `2-letter uppercase code resolves to display name`() {
        val expected = Locale.Builder().setRegion("US").build().getDisplayCountry(Locale.getDefault())
        val result = AddressUtils.getCountryLabelByCountryCode("US")
        assertEquals(expected, result)
    }

    @Test
    fun `2-letter lowercase code resolves to display name`() {
        val expected = Locale.Builder().setRegion("GB").build().getDisplayCountry(Locale.getDefault())
        val result = AddressUtils.getCountryLabelByCountryCode("gb")
        assertEquals(expected, result)
    }

    @Test
    fun `full country name returns the same name (best effort)`() {
        val result = AddressUtils.getCountryLabelByCountryCode("India")
        // In US locale, display name for IN is "India"
        assertEquals("India", result)
    }

    @Test
    fun `ill-formed 2-letter code falls back to input`() {
        val result = AddressUtils.getCountryLabelByCountryCode("ZZ")
        assertEquals("ZZ", result)
    }

    @Test
    fun `trims surrounding whitespace`() {
        val expected = Locale.Builder().setRegion("CA").build().getDisplayCountry(Locale.getDefault())
        val result = AddressUtils.getCountryLabelByCountryCode("  CA  ")
        assertEquals(expected, result)
    }

    @Test
    fun `non-2-letter arbitrary input is returned as-is when not matched`() {
        val input = "Neverland"
        val result = AddressUtils.getCountryLabelByCountryCode(input)
        // There is no ISO country with display name "Neverland"; should echo input
        assertEquals(input, result)
    }
}
