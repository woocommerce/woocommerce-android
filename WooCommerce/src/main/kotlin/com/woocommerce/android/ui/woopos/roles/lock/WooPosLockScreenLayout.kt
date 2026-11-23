package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlin.math.floor

internal data class WooPosLockScreenLayout(
    val keySize: Dp,
    val verticalPadding: Dp,
    val showsIllustration: Boolean,
    val statusHeight: Dp,
) {
    data class Metrics(
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val reducedVerticalPadding: Dp,
        val sectionSpacing: Dp,
        val keySpacing: Dp,
        val illustrationSize: Dp,
    )

    companion object {
        private const val COLUMNS = 3
        private const val ROWS = 4
        private const val MINIMUM_KEY_SCALE = 0.8f
        private val MINIMUM_KEY_SIZE = 60.dp

        @Suppress("LongParameterList")
        fun calculate(
            viewport: DpSize,
            preferredKeySize: Dp,
            titleHeight: Dp,
            statusHeight: Dp,
            isCompactHeight: Boolean,
            metrics: Metrics,
        ): WooPosLockScreenLayout = with(metrics) {
            val contentWidth = viewport.width - horizontalPadding * 2
            val illustratedHeaderHeight = titleHeight + illustrationSize + sectionSpacing

            fun contentHeight(keySize: Dp, headerHeight: Dp): Dp {
                val entryHeight = statusHeight + sectionSpacing + keypadHeight(keySize)
                return headerHeight + sectionSpacing + entryHeight
            }

            val preferredHeaderHeight = if (isCompactHeight) titleHeight else illustratedHeaderHeight
            val fitsPreferred = contentHeight(preferredKeySize, preferredHeaderHeight) + verticalPadding * 2 <=
                viewport.height
            val padding = if (fitsPreferred) verticalPadding else reducedVerticalPadding
            val availableHeight = viewport.height - padding * 2

            fun fittedKeySize(headerHeight: Dp): Dp {
                val headerSpace = headerHeight + sectionSpacing
                val keypadHeight = availableHeight - headerSpace - statusHeight - sectionSpacing
                val widthLimit = (contentWidth - keySpacing * (COLUMNS - 1)) / COLUMNS
                val heightLimit = (keypadHeight - keySpacing * (ROWS - 1)) / ROWS
                val minimum = max(MINIMUM_KEY_SIZE, preferredKeySize * MINIMUM_KEY_SCALE)
                return floor(min(widthLimit, heightLimit).value).dp.coerceIn(minimum, max(minimum, preferredKeySize))
            }

            val illustratedKeySize = fittedKeySize(illustratedHeaderHeight)
            val showsIllustration = !isCompactHeight &&
                contentHeight(illustratedKeySize, illustratedHeaderHeight) <= availableHeight
            WooPosLockScreenLayout(
                keySize = if (showsIllustration) illustratedKeySize else fittedKeySize(titleHeight),
                verticalPadding = padding,
                showsIllustration = showsIllustration,
                statusHeight = statusHeight,
            )
        }

        private fun Metrics.keypadHeight(keySize: Dp): Dp = keySize * ROWS + keySpacing * (ROWS - 1)
    }
}
