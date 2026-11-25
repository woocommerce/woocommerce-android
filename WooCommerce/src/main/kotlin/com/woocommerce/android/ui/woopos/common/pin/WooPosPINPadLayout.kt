package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import kotlin.math.floor

@Composable
internal fun WooPosPINPadLayout(
    preferredKeySize: Dp,
    preferredSpacing: Dp,
    messageHeight: @Composable (width: Dp, spacing: Dp) -> Dp,
    modifier: Modifier = Modifier,
    message: @Composable BoxWithConstraintsScope.(spacing: Dp) -> Unit,
    keypad: @Composable (keySize: Dp) -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val keySpacing = WooPosSpacing.Small.value
        val keypadGapsHeight = keySpacing * (ROWS - 1)
        val preferredHeight = messageHeight(maxWidth, preferredSpacing) + preferredSpacing +
            preferredKeySize * ROWS + keypadGapsHeight
        val compactSpacing = WooPosSpacing.Small.value
        val minimumKeypadHeight = MINIMUM_KEY_SIZE * ROWS + keypadGapsHeight
        val compactHeight = messageHeight(maxWidth, compactSpacing) + compactSpacing + minimumKeypadHeight
        val spacing = when {
            preferredHeight <= maxHeight -> preferredSpacing
            compactHeight <= maxHeight -> compactSpacing
            else -> WooPosSpacing.None.value
        }
        val copyHeight = messageHeight(maxWidth, spacing)
        val fittedKeySize = floor(
            minOf(
                preferredKeySize,
                (maxHeight - copyHeight - spacing - keypadGapsHeight) / ROWS,
                (maxWidth - keySpacing * (COLUMNS - 1)) / COLUMNS,
            ).value,
        ).dp
        val keySize = fittedKeySize.coerceAtLeast(MINIMUM_KEY_SIZE)
        val copyMaxHeight = (maxHeight - keySize * ROWS - keypadGapsHeight - spacing).coerceAtLeast(0.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().heightIn(max = copyMaxHeight),
                contentAlignment = Alignment.Center,
            ) {
                message(spacing)
            }
            keypad(keySize)
        }
    }
}

private const val ROWS = 4
private const val COLUMNS = 3
private val MINIMUM_KEY_SIZE = 48.dp
