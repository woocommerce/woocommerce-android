package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.pin.WooPosPinDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPinPad

@Composable
fun WooPosPinSetupDialog(
    state: WooPosPinSetupState,
    isVisible: Boolean,
    onUIEvent: (WooPosPinSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(R.string.woopos_security_setup),
        onCloseClick = onClose,
        onDismissRequest = onClose,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = WooPosSpacing.Large.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
        ) {
            val title = when (state.stage) {
                WooPosPinSetupState.Stage.CHOOSE -> R.string.woopos_security_choose_pin
                WooPosPinSetupState.Stage.CONFIRM,
                WooPosPinSetupState.Stage.CHECKING -> R.string.woopos_security_confirm_pin
                WooPosPinSetupState.Stage.SUCCESS -> R.string.woopos_security_success
            }
            WooPosText(
                text = stringResource(title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
            if (state.canEnterPIN) {
                WooPosPinDots(state.enteredDigits)
                if (state.mismatch) {
                    WooPosText(
                        text = stringResource(R.string.woopos_security_mismatch),
                        style = WooPosTypography.BodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                WooPosPinPad(
                    enteredDigits = state.enteredDigits,
                    enabled = true,
                    onDigit = { onUIEvent(WooPosPinSetupUIEvent.DigitTapped(it)) },
                    onDelete = { onUIEvent(WooPosPinSetupUIEvent.DeleteTapped) },
                )
                if (state.stage == WooPosPinSetupState.Stage.CONFIRM) {
                    WooPosOutlinedButton(
                        text = stringResource(R.string.back),
                        onClick = { onUIEvent(WooPosPinSetupUIEvent.BackTapped) },
                    )
                }
            } else if (state.stage == WooPosPinSetupState.Stage.CHECKING) {
                WooPosCircularLoadingIndicator(Modifier.size(WooPosIconSize.Large.value).progressSemantics())
            } else {
                WooPosOutlinedButton(text = stringResource(R.string.done), onClick = onClose)
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosPinSetupChoosePreview() {
    PinSetupDialogPreview(WooPosPinSetupState())
}

@WooPosPreview
@Composable
fun WooPosPinSetupConfirmPreview() {
    PinSetupDialogPreview(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CONFIRM))
}

@WooPosPreview
@Composable
fun WooPosPinSetupMismatchPreview() {
    PinSetupDialogPreview(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CONFIRM, mismatch = true))
}

@WooPosPreview
@Composable
fun WooPosPinSetupCheckingPreview() {
    PinSetupDialogPreview(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CHECKING))
}

@WooPosPreview
@Composable
fun WooPosPinSetupSuccessPreview() {
    PinSetupDialogPreview(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.SUCCESS))
}

@Composable
private fun PinSetupDialogPreview(state: WooPosPinSetupState) {
    WooPosTheme {
        WooPosPinSetupDialog(state = state, isVisible = true, onUIEvent = {}, onClose = {})
    }
}
