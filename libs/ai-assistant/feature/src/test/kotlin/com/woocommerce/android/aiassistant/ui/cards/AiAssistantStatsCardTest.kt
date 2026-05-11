package com.woocommerce.android.aiassistant.ui.cards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AiAssistantStatsCardTest {
    @Test
    fun `given empty values, when deciding trend visibility, then chart is hidden`() {
        assertThat(shouldShowStatsTrendChart(emptyList())).isFalse()
    }

    @Test
    fun `given one value, when deciding trend visibility, then chart is hidden`() {
        assertThat(shouldShowStatsTrendChart(listOf(12.0))).isFalse()
    }

    @Test
    fun `given multiple values, when deciding trend visibility, then chart is shown`() {
        assertThat(shouldShowStatsTrendChart(listOf(12.0, 18.0))).isTrue()
    }
}
