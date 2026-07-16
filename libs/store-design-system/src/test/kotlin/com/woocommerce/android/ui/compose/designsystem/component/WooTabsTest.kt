package com.woocommerce.android.ui.compose.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooTabsTest {
    @Test
    fun `given tabs wider than content, when layout is calculated, then overflow remains centered`() {
        val result = calculateWooTabRowLayout(
            rowWidth = 360,
            tabWidths = listOf(134, 134, 134),
            selectedTabIndex = 1,
        )

        assertThat(result.tabPositions).containsExactly(-21, 113, 247)
        assertThat(result.selectedIndicatorPosition).isEqualTo(113)
        assertThat(result.selectedIndicatorWidth).isEqualTo(134)
        assertThat(result.dividerSegments).containsExactly(
            WooTabRowDividerSegment(position = 0, width = 113),
            WooTabRowDividerSegment(position = 247, width = 113),
        )
    }

    @Test
    fun `given tabs narrower than content, when layout is calculated, then tabs remain centered`() {
        val result = calculateWooTabRowLayout(
            rowWidth = 360,
            tabWidths = listOf(80, 80, 80),
            selectedTabIndex = 0,
        )

        assertThat(result.tabPositions).containsExactly(60, 140, 220)
    }
}
