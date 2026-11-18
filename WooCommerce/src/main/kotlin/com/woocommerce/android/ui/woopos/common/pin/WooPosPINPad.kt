package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash

@Composable
fun WooPosPINPad(
    enteredDigits: Int,
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    style: WooPosPINPadStyle = WooPosPINPadStyle.Standard,
) {
    val spacing = when (style) {
        WooPosPINPadStyle.Standard -> WooPosSpacing.Medium.value
        is WooPosPINPadStyle.LockScreen -> style.keySpacing
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                for (column in 1..3) {
                    DigitKey(row * 3 + column, enabled, style, onDigit)
                }
            }
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Spacer(Modifier.size(style.keySize()))
            DigitKey(0, enabled, style, onDigit)
            PINKey(onClick = onDelete, enabled = enabled && enteredDigits > 0, style = style) {
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
fun WooPosPINDots(enteredDigits: Int) {
    val label = stringResource(R.string.woopos_lock_pin_entry)
    val description = stringResource(
        R.string.woopos_lock_digits_entered,
        enteredDigits,
        WooPosPINHash.PIN_LENGTH,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = label
            stateDescription = description
        },
    ) {
        repeat(WooPosPINHash.PIN_LENGTH) { index ->
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
private fun DigitKey(digit: Int, enabled: Boolean, style: WooPosPINPadStyle, onDigit: (Int) -> Unit) {
    PINKey(enabled = enabled, style = style, onClick = { onDigit(digit) }) {
        WooPosText(text = digit.toString(), style = WooPosTypography.Heading, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PINKey(
    enabled: Boolean,
    style: WooPosPINPadStyle,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    when (style) {
        WooPosPINPadStyle.Standard -> Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            color = MaterialTheme.colorScheme.surface,
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else WooPosTheme.colors.onDisabledContainer,
            modifier = Modifier
                .width(style.keySize())
                .heightIn(min = style.keySize())
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
        is WooPosPINPadStyle.LockScreen -> LockScreenKey(enabled, style, onClick, content)
    }
}

@Composable
private fun LockScreenKey(
    enabled: Boolean,
    style: WooPosPINPadStyle.LockScreen,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val container = when {
        style.showsDisabledKeys && isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceDim
        style.showsDisabledKeys -> WooPosTheme.colors.disabledContainer
        isPressed -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceDim
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(WooPosCornerRadius.Large.value),
        color = container,
        contentColor = if (style.showsDisabledKeys) {
            WooPosTheme.colors.onDisabledContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        interactionSource = interactionSource,
        modifier = Modifier
            .width(style.keySize)
            .heightIn(min = style.keySize)
            .fillMaxHeight(),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun WooPosPINPadStyle.keySize(): Dp = when (this) {
    WooPosPINPadStyle.Standard -> WooPosComponentSize.Small.value
    is WooPosPINPadStyle.LockScreen -> keySize
}

private val PIN_DOT_SIZE = 22.dp
private val PIN_DOT_BORDER_WIDTH = 2.dp
