package com.woocommerce.android.ui.compose.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooTabsTest {
    @Test
    fun `given tabs wider than content, when layout is calculated, then tabs start at content start`() {
        val result = calculateWooTabRowLayout(
            rowWidth = 360,
            horizontalPadding = 24,
            tabWidths = listOf(134, 134, 134),
            selectedTabIndex = 1,
        )

        assertThat(result.tabPositions).containsExactly(24, 158, 292)
        assertThat(result.selectedIndicatorPosition).isEqualTo(158)
        assertThat(result.selectedIndicatorWidth).isEqualTo(134)
        assertThat(result.dividerSegments).containsExactly(
            WooTabRowDividerSegment(position = 0, width = 158),
            WooTabRowDividerSegment(position = 292, width = 68),
        )
    }

    @Test
    fun `given tabs narrower than content, when layout is calculated, then tabs remain centered`() {
        val result = calculateWooTabRowLayout(
            rowWidth = 360,
            horizontalPadding = 24,
            tabWidths = listOf(80, 80, 80),
            selectedTabIndex = 0,
        )

        assertThat(result.tabPositions).containsExactly(60, 140, 220)
    }
}
