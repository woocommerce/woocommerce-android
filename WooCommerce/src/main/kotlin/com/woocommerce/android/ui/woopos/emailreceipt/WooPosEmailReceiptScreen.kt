package com.woocommerce.android.ui.woopos.emailreceipt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosEmailReceiptScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel = hiltViewModel<WooPosEmailReceiptViewModel>()
    val state = viewModel.state.collectAsState().value

    WooPosEmailReceiptScreen(
        state = state,
        onEmailAddressChanged = { viewModel.onUIEvent(WooPosEmailReceiptUIEvent.EmailChanged(it)) },
        onSendReceiptClicked = { viewModel.onUIEvent(WooPosEmailReceiptUIEvent.SendEmailClicked) },
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onEmailSent = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
    )
}

@Composable
private fun WooPosEmailReceiptScreen(
    state: WooPosEmailReceiptState,
    onEmailAddressChanged: (String) -> Unit,
    onSendReceiptClicked: () -> Unit,
    onEmailSent: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_email_receipt_title),
            onBackClicked = onBackClicked,
        )
        when (state) {
            is WooPosEmailReceiptState.Email ->
                EmailState(
                    state = state,
                    onEmailAddressChanged = onEmailAddressChanged,
                    onSendReceiptClicked = onSendReceiptClicked
                )

            is WooPosEmailReceiptState.Sent -> onEmailSent()
        }
    }
}

@Composable
private fun EmailState(
    state: WooPosEmailReceiptState.Email,
    onEmailAddressChanged: (String) -> Unit,
    onSendReceiptClicked: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val (email, error, button) = createRefs()
        val standardMargin = WooPosSpacing.Medium.value.toAdaptivePadding()
        val topMargin = 72.dp.toAdaptivePadding()

        WooPosInputField(
            value = state.email,
            onValueChange = onEmailAddressChanged,
            label = stringResource(R.string.woopos_email_receipt_email_label),
            contentAlignment = Alignment.Center,
            textStyle = WooPosTypography.Heading,
            textColor = MaterialTheme.colorScheme.onSurface,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier
                .constrainAs(email) {
                    top.linkTo(parent.top, margin = topMargin)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .focusRequester(focusRequester),
        )

        if (state.errorMessage != null) {
            WooPosText(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(error) {
                    top.linkTo(email.bottom, margin = WooPosSpacing.Small.value)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }

        WooPosButton(
            text = state.button.text,
            onClick = onSendReceiptClicked,
            state = when (state.button.status) {
                WooPosEmailReceiptState.Email.Button.Status.ENABLED -> WooPosButtonState.ENABLED
                WooPosEmailReceiptState.Email.Button.Status.DISABLED -> WooPosButtonState.DISABLED
                WooPosEmailReceiptState.Email.Button.Status.LOADING -> WooPosButtonState.LOADING
            },
            modifier = Modifier.constrainAs(button) {
                bottom.linkTo(parent.bottom, margin = WooPosSpacing.Medium.value)
                start.linkTo(parent.start, margin = standardMargin)
                end.linkTo(parent.end, margin = standardMargin)
                width = Dimension.fillToConstraints
            },
        )
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosTotalsPaymentReceiptScreen() {
    WooPosTheme {
        WooPosEmailReceiptScreen(
            state = WooPosEmailReceiptState.Email(
                email = "email@google.com",
                errorMessage = null,
                button = WooPosEmailReceiptState.Email.Button(
                    text = "Send",
                    status = WooPosEmailReceiptState.Email.Button.Status.ENABLED
                )
            ),
            onEmailAddressChanged = {},
            onSendReceiptClicked = {},
            onBackClicked = {},
            onEmailSent = {},
        )
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosTotalsPaymentReceiptWithLabelScreen() {
    WooPosTheme {
        WooPosEmailReceiptScreen(
            state = WooPosEmailReceiptState.Email(
                email = "",
                errorMessage = null,
                button = WooPosEmailReceiptState.Email.Button(
                    text = "Send",
                    status = WooPosEmailReceiptState.Email.Button.Status.ENABLED
                )
            ),
            onEmailAddressChanged = {},
            onSendReceiptClicked = {},
            onEmailSent = {},
            onBackClicked = {},
        )
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosTotalsPaymentReceiptWithErrorScreen() {
    WooPosTheme {
        WooPosEmailReceiptScreen(
            state = WooPosEmailReceiptState.Email(
                email = "email@google.com",
                errorMessage = "Invalid email",
                button = WooPosEmailReceiptState.Email.Button(
                    text = "Send",
                    status = WooPosEmailReceiptState.Email.Button.Status.ENABLED
                )
            ),
            onEmailAddressChanged = {},
            onSendReceiptClicked = {},
            onBackClicked = {},
            onEmailSent = {},
        )
    }
}
