package com.woocommerce.android.ui.woopos.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.pin.WooPosPinDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPinPad

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
            WooPosPinDots(state.enteredDigits)
            LockStatus(state)
            WooPosPinPad(
                enteredDigits = state.enteredDigits,
                enabled = state.canEnterPIN,
                onDigit = { onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it)) },
                onDelete = { onUIEvent(WooPosLockScreenUIEvent.DeleteTapped) },
            )
            TextButton(onClick = { onUIEvent(WooPosLockScreenUIEvent.ForgotPINTapped) }) {
                WooPosText(stringResource(R.string.woopos_lock_forgot_pin), style = WooPosTypography.BodyLarge)
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosLockScreenPreview() {
    WooPosTheme {
        WooPosLockScreen(state = WooPosLockScreenState(isLocked = true), onUIEvent = {})
    }
}

@Composable
private fun LockStatus(state: WooPosLockScreenState) {
    val message = when {
        state.remainingSeconds > 0 -> stringResource(
            R.string.woopos_lock_wait,
            state.remainingSeconds / 60,
            state.remainingSeconds % 60,
        )
        state.isChecking -> stringResource(R.string.woopos_lock_checking)
        state.error == WooPosLockError.INCORRECT -> stringResource(R.string.woopos_lock_incorrect)
        state.error == WooPosLockError.STORAGE -> stringResource(R.string.woopos_lock_storage_error)
        state.error == WooPosLockError.RECOVERY_UNAVAILABLE -> stringResource(R.string.woopos_lock_recovery_pending)
        else -> null
    }
    val isError = state.remainingSeconds == 0L && !state.isChecking &&
        (state.error == WooPosLockError.INCORRECT || state.error == WooPosLockError.STORAGE)
    if (message != null) {
        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (state.remainingSeconds > 0 && state.error == WooPosLockError.RECOVERY_UNAVAILABLE) {
        WooPosText(
            text = stringResource(R.string.woopos_lock_recovery_pending),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}
