package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlin.math.floor

/** Fits the lock content without scrolling: hides the illustration and shrinks keys before content must scroll. */
internal data class WooPosLockScreenLayout(
    val keySize: Dp,
    val verticalPadding: Dp,
    val showsIllustration: Boolean,
) {
    data class Metrics(
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val reducedVerticalPadding: Dp,
        val sectionSpacing: Dp,
        val columnSpacing: Dp,
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
            isHorizontal: Boolean,
            metrics: Metrics,
        ): WooPosLockScreenLayout = with(metrics) {
            val contentWidth = viewport.width - horizontalPadding * 2
            val keypadWidth = if (isHorizontal) (contentWidth - columnSpacing) / 2 else contentWidth
            val illustratedHeaderHeight = titleHeight + illustrationSize + sectionSpacing

            fun contentHeight(keySize: Dp, headerHeight: Dp): Dp {
                val entryHeight = statusHeight + sectionSpacing + keypadHeight(keySize)
                return if (isHorizontal) max(headerHeight, entryHeight) else headerHeight + sectionSpacing + entryHeight
            }

            val fitsPreferred = contentHeight(preferredKeySize, illustratedHeaderHeight) + verticalPadding * 2 <=
                viewport.height
            val padding = if (fitsPreferred) verticalPadding else reducedVerticalPadding
            val availableHeight = viewport.height - padding * 2

            fun fittedKeySize(headerHeight: Dp): Dp {
                val headerSpace = if (isHorizontal) 0.dp else headerHeight + sectionSpacing
                val keypadHeight = availableHeight - headerSpace - statusHeight - sectionSpacing
                val widthLimit = (keypadWidth - keySpacing * (COLUMNS - 1)) / COLUMNS
                val heightLimit = (keypadHeight - keySpacing * (ROWS - 1)) / ROWS
                val minimum = max(MINIMUM_KEY_SIZE, preferredKeySize * MINIMUM_KEY_SCALE)
                return floor(min(widthLimit, heightLimit).value).dp.coerceIn(minimum, max(minimum, preferredKeySize))
            }

            val illustratedKeySize = fittedKeySize(illustratedHeaderHeight)
            val showsIllustration = contentHeight(illustratedKeySize, illustratedHeaderHeight) <= availableHeight
            WooPosLockScreenLayout(
                keySize = if (showsIllustration) illustratedKeySize else fittedKeySize(titleHeight),
                verticalPadding = padding,
                showsIllustration = showsIllustration,
            )
        }

        private fun Metrics.keypadHeight(keySize: Dp): Dp = keySize * ROWS + keySpacing * (ROWS - 1)
    }
}
