package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

const val WOO_POS_PIN_LENGTH = 4

@Composable
fun WooPosPinPad(
    enteredDigits: Int,
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
            ) {
                for (column in 1..3) {
                    DigitKey(row * 3 + column, enabled, onDigit)
                }
            }
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        ) {
            Spacer(Modifier.size(WooPosComponentSize.Small.value))
            DigitKey(0, enabled, onDigit)
            PinKey(onClick = onDelete, enabled = enabled && enteredDigits > 0) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_woopos_backspace),
                    contentDescription = stringResource(R.string.woopos_lock_delete_digit),
                    modifier = Modifier.size(WooPosIconSize.Small.value),
                )
            }
        }
    }
}

@Composable
fun WooPosPinDots(enteredDigits: Int) {
    val label = stringResource(R.string.woopos_lock_pin_entry)
    val description = stringResource(
        R.string.woopos_lock_digits_entered,
        enteredDigits,
        WOO_POS_PIN_LENGTH,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = label
            stateDescription = description
        },
    ) {
        repeat(WOO_POS_PIN_LENGTH) { index ->
            val filled = index < enteredDigits
            Box(
                Modifier
                    .size(PIN_DOT_SIZE)
                    .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .border(
                        PIN_DOT_BORDER_WIDTH,
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    )
            )
        }
    }
}

@Composable
private fun DigitKey(digit: Int, enabled: Boolean, onDigit: (Int) -> Unit) {
    PinKey(enabled = enabled, onClick = { onDigit(digit) }) {
        WooPosText(text = digit.toString(), style = WooPosTypography.Heading, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PinKey(enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        color = MaterialTheme.colorScheme.surface,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else WooPosTheme.colors.onDisabledContainer,
        modifier = Modifier
            .width(WooPosComponentSize.Small.value)
            .heightIn(min = WooPosComponentSize.Small.value)
            .fillMaxHeight(),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private val PIN_DOT_SIZE = 22.dp
private val PIN_DOT_BORDER_WIDTH = 2.dp
