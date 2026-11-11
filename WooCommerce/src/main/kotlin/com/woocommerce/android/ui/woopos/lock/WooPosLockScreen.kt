package com.woocommerce.android.ui.woopos.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

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
                securePolicy = SecureFlagPolicy.SecureOn,
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
            LockStatus(state)
            Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
                    ) {
                        for (column in 1..3) {
                            DigitKey(row * 3 + column, state.canEnterPIN, onUIEvent)
                        }
                    }
                }
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
                ) {
                    Spacer(Modifier.width(WooPosComponentSize.Small.value))
                    DigitKey(0, state.canEnterPIN, onUIEvent)
                    PinKey(
                        onClick = { onUIEvent(WooPosLockScreenUIEvent.DeleteTapped) },
                        enabled = state.canEnterPIN && state.enteredDigits > 0,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_woopos_backspace),
                            contentDescription = stringResource(R.string.woopos_lock_delete_digit),
                            modifier = Modifier.size(WooPosIconSize.Small.value),
                        )
                    }
                }
            }
            TextButton(
                onClick = { onUIEvent(WooPosLockScreenUIEvent.ForgotPINTapped) },
                enabled = !state.isChecking,
            ) {
                WooPosText(stringResource(R.string.woopos_lock_forgot_pin), style = WooPosTypography.BodyLarge)
            }
        }
    }
}

@Composable
private fun LockStatus(state: WooPosLockScreenState) {
    val checking = stringResource(R.string.woopos_lock_checking)
    val incorrect = stringResource(R.string.woopos_lock_incorrect)
    val storage = stringResource(R.string.woopos_lock_storage_error)
    val recovery = stringResource(R.string.woopos_lock_recovery_pending)
    val wait = stringResource(R.string.woopos_lock_wait, state.remainingSeconds / 60, state.remainingSeconds % 60)
    val longestWait = stringResource(R.string.woopos_lock_wait, 60, 0)
    val message = when {
        state.remainingSeconds > 0 -> wait
        state.isChecking -> checking
        state.error == WooPosLockError.INCORRECT -> incorrect
        state.error == WooPosLockError.STORAGE -> storage
        state.error == WooPosLockError.RECOVERY_UNAVAILABLE -> recovery
        else -> null
    }
    val showRecovery = state.error == WooPosLockError.RECOVERY_UNAVAILABLE && state.remainingSeconds > 0
    val isError = state.remainingSeconds == 0L && !state.isChecking &&
        (state.error == WooPosLockError.INCORRECT || state.error == WooPosLockError.STORAGE)
    val keypadWidth = WooPosComponentSize.Small.value * 3 + WooPosSpacing.Medium.value * 2
    val textMeasurer = rememberTextMeasurer()
    val neutralStyle = WooPosTypography.BodyMedium.style
    val errorStyle = WooPosTypography.BodyLarge.style
    BoxWithConstraints(modifier = Modifier.width(keypadWidth), contentAlignment = Alignment.Center) {
        val textConstraints = Constraints(maxWidth = constraints.maxWidth)
        val recoveryHeight = textMeasurer.measure(recovery, neutralStyle, constraints = textConstraints).size.height
        val waitHeight = textMeasurer.measure(longestWait, neutralStyle, constraints = textConstraints).size.height
        val gap = with(LocalDensity.current) { WooPosSpacing.Small.value.roundToPx() }
        val reservedHeight = maxOf(
            textMeasurer.measure(checking, neutralStyle, constraints = textConstraints).size.height,
            textMeasurer.measure(incorrect, errorStyle, constraints = textConstraints).size.height,
            textMeasurer.measure(storage, errorStyle, constraints = textConstraints).size.height,
            waitHeight + gap + recoveryHeight,
        )
        Box(
            modifier = Modifier.heightIn(min = with(LocalDensity.current) { reservedHeight.toDp() }),
            contentAlignment = Alignment.Center,
        ) {
            LockStatusMessages(message, recovery.takeIf { showRecovery }, isError)
        }
    }
}

@Composable
private fun LockStatusMessages(
    message: String?,
    recovery: String? = null,
    isError: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
    ) {
        message?.let {
            WooPosText(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                style = if (isError) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        recovery?.let {
            WooPosText(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                style = WooPosTypography.BodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PinDots(enteredDigits: Int) {
    val label = stringResource(R.string.woopos_lock_pin_entry)
    val description = stringResource(
        R.string.woopos_lock_digits_entered,
        enteredDigits,
        WooPosLockScreenState.PIN_LENGTH,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = label
            stateDescription = description
        },
    ) {
        repeat(WooPosLockScreenState.PIN_LENGTH) { index ->
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
private fun DigitKey(digit: Int, enabled: Boolean, onUIEvent: (WooPosLockScreenUIEvent) -> Unit) {
    PinKey(enabled = enabled, onClick = { onUIEvent(WooPosLockScreenUIEvent.DigitTapped(digit)) }) {
        WooPosText(
            text = digit.toString(),
            modifier = Modifier.fillMaxWidth(),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
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

@WooPosPreview
@Composable
fun WooPosLockScreenPreview() {
    WooPosTheme {
        WooPosLockScreen(state = WooPosLockScreenState(isLocked = true), onUIEvent = {})
    }
}
