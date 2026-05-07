package com.woocommerce.android.aiassistant.ui.cards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantStatsTrendChartTest {
    @Test
    fun `given empty points, when normalized, then empty list is returned`() {
        assertThat(normalizeStatsTrendChartPoints(emptyList())).isEmpty()
    }

    @Test
    fun `given single point, when normalized, then centered point is returned`() {
        assertThat(normalizeStatsTrendChartPoints(listOf(12.0))).containsExactly(0.5f)
    }

    @Test
    fun `given all zero points, when normalized, then centered points are returned`() {
        assertThat(normalizeStatsTrendChartPoints(listOf(0.0, 0.0, 0.0))).containsExactly(0.5f, 0.5f, 0.5f)
    }

    @Test
    fun `given flat non zero points, when normalized, then centered points are returned`() {
        assertThat(normalizeStatsTrendChartPoints(listOf(4.0, 4.0, 4.0))).containsExactly(0.5f, 0.5f, 0.5f)
    }

    @Test
    fun `given negative values, when normalized, then values are normalized in order`() {
        assertThat(normalizeStatsTrendChartPoints(listOf(-10.0, -5.0, -1.0))).containsExactly(0.0f, 5f / 9f, 1.0f)
    }

    @Test
    fun `given mixed negative and positive values, when normalized, then min maps to 0 and max maps to 1`() {
        assertThat(normalizeStatsTrendChartPoints(listOf(-5.0, 0.0, 5.0))).containsExactly(0.0f, 0.5f, 1.0f)
    }
}
