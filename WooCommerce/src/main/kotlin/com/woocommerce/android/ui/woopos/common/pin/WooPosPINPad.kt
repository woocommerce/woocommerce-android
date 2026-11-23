package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash

@Composable
fun WooPosPINPad(
    enteredDigits: Int,
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    style: WooPosPINPadStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(style.keySpacing)) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(style.keySpacing),
            ) {
                for (column in 1..3) {
                    DigitKey(row * 3 + column, enabled, style, onDigit)
                }
            }
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(style.keySpacing),
        ) {
            Spacer(Modifier.size(style.keySize))
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

/** Hidden dots stay composed, for example under a message, so the next digit still pops in. */
@Composable
fun WooPosPINDots(enteredDigits: Int, modifier: Modifier = Modifier, isHidden: Boolean = false) {
    val label = stringResource(R.string.woopos_lock_pin_entry)
    val description = stringResource(R.string.woopos_lock_digits_entered, enteredDigits, WooPosPINHash.PIN_LENGTH)
    Row(
        horizontalArrangement = Arrangement.spacedBy(PIN_DOT_SPACING),
        modifier = modifier.clearAndSetSemantics {
            if (!isHidden) {
                contentDescription = label
                stateDescription = description
            }
        },
    ) {
        repeat(WooPosPINHash.PIN_LENGTH) { index -> PINDot(filled = index < enteredDigits) }
    }
}

@Composable
private fun PINDot(filled: Boolean) {
    Box(Modifier.size(PIN_DOT_SIZE)) {
        Box(Modifier.matchParentSize().border(PIN_DOT_BORDER_WIDTH, WooPosTheme.colors.outline, CircleShape))
        AnimatedVisibility(
            visible = filled,
            enter = scaleIn(tween(PIN_DOT_POP_MILLIS, easing = FastOutSlowInEasing), initialScale = PIN_DOT_POP_SCALE),
            exit = fadeOut(tween(PIN_DOT_CLEAR_MILLIS)),
        ) {
            Box(Modifier.size(PIN_DOT_SIZE).background(MaterialTheme.colorScheme.onSurface, CircleShape))
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

private const val PIN_DOT_POP_MILLIS = 200
private const val PIN_DOT_POP_SCALE = 1.2f
internal const val PIN_DOT_CLEAR_MILLIS = 120
internal val PIN_DOT_SIZE = 20.dp
private val PIN_DOT_SPACING = 12.dp
private val PIN_DOT_BORDER_WIDTH = 1.dp
