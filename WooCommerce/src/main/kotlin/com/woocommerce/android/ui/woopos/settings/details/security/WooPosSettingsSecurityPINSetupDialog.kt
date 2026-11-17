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
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Stage

@Composable
fun WooPosSettingsSecurityPINSetupDialog(
    state: WooPosSettingsSecurityPINSetupState,
    isVisible: Boolean,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
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
            when (state.stage) {
                Stage.CHOOSE -> PINSetupEntry(
                    state = state,
                    title = stringResource(R.string.woopos_security_choose_pin),
                    onUIEvent = onUIEvent,
                )
                Stage.CONFIRM,
                Stage.MISMATCH -> PINSetupEntry(
                    state = state,
                    title = stringResource(R.string.woopos_security_confirm_pin),
                    onUIEvent = onUIEvent,
                    showBack = true,
                    showMismatch = state.stage == Stage.MISMATCH,
                )
                Stage.CHECKING -> {
                    PINSetupHeading(stringResource(R.string.woopos_security_confirm_pin))
                    WooPosCircularLoadingIndicator(Modifier.size(WooPosIconSize.Large.value).progressSemantics())
                }
                Stage.SUCCESS -> {
                    PINSetupHeading(stringResource(R.string.woopos_security_success))
                    WooPosOutlinedButton(text = stringResource(R.string.done), onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun PINSetupEntry(
    state: WooPosSettingsSecurityPINSetupState,
    title: String,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    showBack: Boolean = false,
    showMismatch: Boolean = false,
) {
    PINSetupHeading(title)
    WooPosPINDots(state.enteredDigits)
    if (showMismatch) {
        WooPosText(
            text = stringResource(R.string.woopos_security_mismatch),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    WooPosPINPad(
        enteredDigits = state.enteredDigits,
        enabled = true,
        onDigit = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(it)) },
        onDelete = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped) },
    )
    if (showBack) {
        WooPosOutlinedButton(
            text = stringResource(R.string.back),
            onClick = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped) },
        )
    }
}

@Composable
private fun PINSetupHeading(title: String) {
    WooPosText(
        text = title,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
    )
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupChoosePreview() {
    PINSetupDialogPreview(WooPosSettingsSecurityPINSetupState())
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupConfirmPreview() {
    PINSetupDialogPreview(WooPosSettingsSecurityPINSetupState(stage = Stage.CONFIRM))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupMismatchPreview() {
    PINSetupDialogPreview(WooPosSettingsSecurityPINSetupState(stage = Stage.MISMATCH))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupCheckingPreview() {
    PINSetupDialogPreview(WooPosSettingsSecurityPINSetupState(stage = Stage.CHECKING))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupSuccessPreview() {
    PINSetupDialogPreview(WooPosSettingsSecurityPINSetupState(stage = Stage.SUCCESS))
}

@Composable
private fun PINSetupDialogPreview(state: WooPosSettingsSecurityPINSetupState) {
    WooPosTheme {
        WooPosSettingsSecurityPINSetupDialog(state = state, isVisible = true, onUIEvent = {}, onClose = {})
    }
}
