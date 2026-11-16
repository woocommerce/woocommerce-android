package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash

@Composable
fun WooPosLockScreenDialog(
    state: WooPosLockScreenState,
    onUIEvent: (WooPosLockScreenUIEvent) -> Unit,
) {
    if (state.isLocked) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            WooPosLockScreen(state = state, onUIEvent = onUIEvent)
        }
    }
}

@Composable
fun WooPosLockScreen(
    state: WooPosLockScreenState,
    onUIEvent: (WooPosLockScreenUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(WooPosSpacing.XLarge.value),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_lock_enter_pin),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            PinDots(state.enteredDigits)
            Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
                        for (column in 1..3) {
                            DigitKey(row * 3 + column, onUIEvent)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
                    Spacer(Modifier.size(WooPosComponentSize.Small.value))
                    DigitKey(0, onUIEvent)
                    PinKey(
                        onClick = { onUIEvent(WooPosLockScreenUIEvent.DeleteTapped) },
                        enabled = state.enteredDigits > 0,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_woopos_backspace),
                            contentDescription = stringResource(R.string.woopos_lock_delete_digit),
                            modifier = Modifier.size(WooPosIconSize.Small.value),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDots(enteredDigits: Int) {
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
private fun DigitKey(digit: Int, onUIEvent: (WooPosLockScreenUIEvent) -> Unit) {
    PinKey(onClick = { onUIEvent(WooPosLockScreenUIEvent.DigitTapped(digit)) }) {
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
        modifier = Modifier.size(WooPosComponentSize.Small.value),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private val PIN_DOT_SIZE = 22.dp
private val PIN_DOT_BORDER_WIDTH = 2.dp

@WooPosPreview
@Composable
fun WooPosLockScreenPreview() {
    WooPosTheme {
        WooPosLockScreen(state = WooPosLockScreenState(isLocked = true), onUIEvent = {})
    }
}
