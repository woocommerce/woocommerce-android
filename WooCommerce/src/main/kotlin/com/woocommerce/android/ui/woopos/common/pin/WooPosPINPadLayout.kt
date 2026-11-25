package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        val viewportHeight = maxHeight
        val keySpacing = WooPosSpacing.Small.value
        val keypadGapsHeight = keySpacing * (ROWS - 1)
        val preferredHeight = messageHeight(maxWidth, preferredSpacing) + preferredSpacing +
            preferredKeySize * ROWS + keypadGapsHeight
        val spacing = if (preferredHeight <= maxHeight) preferredSpacing else WooPosSpacing.Small.value
        val copyHeight = messageHeight(maxWidth, spacing)
        val fittedKeySize = floor(
            minOf(
                preferredKeySize,
                (maxHeight - copyHeight - spacing - keypadGapsHeight) / ROWS,
                (maxWidth - keySpacing * (COLUMNS - 1)) / COLUMNS,
            ).value,
        ).dp
        val minimumPadWidth = MINIMUM_KEY_SIZE * COLUMNS + keySpacing * (COLUMNS - 1)
        val canUseColumns = maxWidth > maxHeight &&
            maxWidth >= minimumPadWidth + MINIMUM_MESSAGE_WIDTH + spacing
        if (fittedKeySize < MINIMUM_KEY_SIZE && canUseColumns) {
            val columnKeySize = minOf(
                preferredKeySize,
                (maxHeight - keypadGapsHeight) / ROWS,
                (maxWidth - MINIMUM_MESSAGE_WIDTH - spacing - keySpacing * (COLUMNS - 1)) / COLUMNS,
            ).coerceAtLeast(MINIMUM_KEY_SIZE)
            val columnMessageWidth = maxWidth - columnKeySize * COLUMNS - keySpacing * (COLUMNS - 1) - spacing
            val messageScroll = if (messageHeight(columnMessageWidth, spacing) > maxHeight) {
                Modifier.verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                BoxWithConstraints(
                    Modifier.weight(1f).heightIn(max = viewportHeight).then(messageScroll),
                    contentAlignment = Alignment.Center,
                ) {
                    message(spacing)
                }
                keypad(columnKeySize)
            }
        } else {
            val keySize = fittedKeySize.coerceAtLeast(MINIMUM_KEY_SIZE)
            val copyMaxHeight = (maxHeight - keySize * ROWS - keypadGapsHeight - spacing).coerceAtLeast(0.dp)
            val messageScroll = if (copyHeight > copyMaxHeight) {
                Modifier.verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().heightIn(max = copyMaxHeight).then(messageScroll),
                    contentAlignment = Alignment.Center,
                ) {
                    message(spacing)
                }
                keypad(keySize)
            }
        }
    }
}

private const val ROWS = 4
private const val COLUMNS = 3
private val MINIMUM_KEY_SIZE = 48.dp
private val MINIMUM_MESSAGE_WIDTH = 80.dp
