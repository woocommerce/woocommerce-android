package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ShouldRequireITNTest {

    private lateinit var shouldRequireITN: ShouldRequireITN

    @Before
    fun setup() {
        shouldRequireITN = ShouldRequireITN()
    }

    @Test
    fun `when destination is Canada, then ITN is not required regardless of value`() {
        // Given
        val destinationCountry = "CA"
        val lowValue = 100f
        val highValue = 5000f

        // When/Then
        assertThat(shouldRequireITN(destinationCountry, lowValue)).isFalse()
        assertThat(shouldRequireITN(destinationCountry, highValue)).isFalse()
    }

    @Test
    fun `when destination is Iran, then ITN is required regardless of value`() {
        // Given
        val destinationCountry = "IR"
        val lowValue = 100f
        val highValue = 5000f

        // When/Then
        assertThat(shouldRequireITN(destinationCountry, lowValue)).isTrue()
        assertThat(shouldRequireITN(destinationCountry, highValue)).isTrue()
    }

    @Test
    fun `when destination is North Korea, then ITN is required regardless of value`() {
        // Given
        val destinationCountry = "KP"
        val lowValue = 100f

        // When/Then
        assertThat(shouldRequireITN(destinationCountry, lowValue)).isTrue()
    }

    @Test
    fun `when destination is Syria, then ITN is required regardless of value`() {
        // Given
        val destinationCountry = "SY"
        val lowValue = 100f

        // When/Then
        assertThat(shouldRequireITN(destinationCountry, lowValue)).isTrue()
    }

    @Test
    fun `when destination is Cuba, then ITN is required regardless of value`() {
        // Given
        val destinationCountry = "CU"
        val lowValue = 100f

        // When/Then
        assertThat(shouldRequireITN(destinationCountry, lowValue)).isTrue()
    }

    @Test
    fun `when destination is Sudan, then ITN is required regardless of value`() {
        // Given
        val destinationCountry = "SD"
        val lowValue = 100f

        // When/Then
        assertThat(shouldRequireITN(destinationCountry, lowValue)).isTrue()
    }

    @Test
    fun `when destination is other country and value is below threshold, then ITN is not required`() {
        // Given
        val destinationCountry = "FR"
        val belowThresholdValue = 2499f

        // When
        val result = shouldRequireITN(destinationCountry, belowThresholdValue)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `when destination is other country and value equals threshold, then ITN is not required`() {
        // Given
        val destinationCountry = "DE"
        val thresholdValue = 2500f

        // When
        val result = shouldRequireITN(destinationCountry, thresholdValue)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `when destination is other country and value is above threshold, then ITN is required`() {
        // Given
        val destinationCountry = "JP"
        val aboveThresholdValue = 2501f

        // When
        val result = shouldRequireITN(destinationCountry, aboveThresholdValue)

        // Then
        assertThat(result).isTrue()
    }
}
