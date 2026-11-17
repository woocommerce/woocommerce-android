package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(WooPosSpacing.XLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value, Alignment.CenterVertically),
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_lock_enter_pin),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        WooPosPINDots(state.enteredDigits)
        LockStatus(state.status)
        WooPosPINPad(
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

@WooPosPreview
@Composable
fun WooPosLockScreenPreview() {
    WooPosTheme {
        WooPosLockScreen(state = WooPosLockScreenState(isLocked = true), onUIEvent = {})
    }
}

@Composable
private fun LockStatus(status: WooPosLockScreenState.Status) {
    when (status) {
        WooPosLockScreenState.Status.Idle -> Unit
        WooPosLockScreenState.Status.Checking ->
            WooPosCircularLoadingIndicator(Modifier.size(WooPosIconSize.Large.value).progressSemantics())
        WooPosLockScreenState.Status.Incorrect -> LockStatusMessage(
            message = stringResource(R.string.woopos_lock_incorrect),
            isError = true,
        )
        WooPosLockScreenState.Status.StorageUnavailable -> LockStatusMessage(
            message = stringResource(R.string.woopos_lock_storage_error),
            isError = true,
        )
        is WooPosLockScreenState.Status.Wait -> LockStatusMessage(
            message = stringResource(
                R.string.woopos_lock_wait,
                status.remainingSeconds / 60,
                status.remainingSeconds % 60,
            ),
        )
    }
}

@Composable
private fun LockStatusMessage(message: String, isError: Boolean = false) {
    WooPosText(
        text = message,
        style = WooPosTypography.BodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
