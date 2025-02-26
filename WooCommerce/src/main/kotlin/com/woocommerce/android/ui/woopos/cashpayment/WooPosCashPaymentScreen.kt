package com.woocommerce.android.ui.woopos.cashpayment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosMoneyInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import org.wordpress.android.fluxc.model.WCSettingsModel
import java.math.BigDecimal

@Composable
fun WooPosCashPaymentScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel = hiltViewModel<WooPosCashPaymentViewModel>()
    val state = viewModel.state.collectAsState().value

    WooPosCashPaymentScreen(
        state = state,
        onAmountChanged = { viewModel.onUIEvent(WooPosCashPaymentUIEvent.AmountChanged(it)) },
        onCompleteOrderClicked = { viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked) },
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onOrderComplete = { onNavigationEvent(WooPosNavigationEvent.OpenHomeFromCashPaymentAfterSuccessfulPayment) },
    )
    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }
}

@Composable
fun WooPosCashPaymentScreen(
    state: WooPosCashPaymentState,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCompleteOrderClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onOrderComplete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_cash_payment_title),
            onBackClicked = onBackClicked,
        )

        when (state) {
            is WooPosCashPaymentState.Collecting -> {
                Collecting(
                    state = state,
                    onAmountChanged = onAmountChanged,
                    onCompleteOrderClicked = onCompleteOrderClicked,
                )
            }

            WooPosCashPaymentState.Complete -> onOrderComplete()
            WooPosCashPaymentState.Initiating -> {
                // show nothing
            }
        }
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun Collecting(
    state: WooPosCashPaymentState.Collecting,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCompleteOrderClicked: () -> Unit,
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
        val (input, total, error, changeDue, button) = createRefs()
        val standardMargin = WooPosSpacing.Medium.value.toAdaptivePadding()
        val smallMargin = WooPosSpacing.Small.value.toAdaptivePadding()

        WooPosText(
            text = state.totalText,
            style = WooPosTypography.BodyLarge,
            modifier = Modifier
                .constrainAs(total) {
                    top.linkTo(parent.top, margin = WooPosSpacing.XSmall.value)
                    start.linkTo(parent.start, margin = 64.dp)
                }
        )

        var inputText by remember { mutableStateOf(state.enteredAmount) }

        val marginBetweenTotalAndInput = 48.dp.toAdaptivePadding()
        WooPosMoneyInputField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .constrainAs(input) {
                    top.linkTo(total.bottom, margin = marginBetweenTotalAndInput)
                    start.linkTo(parent.start, margin = standardMargin)
                    end.linkTo(parent.end, margin = standardMargin)
                },
            value = inputText,
            onValueChange = {
                onAmountChanged(it)
                inputText = it
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            textStyle = WooPosTypography.Heading,
            textColor = MaterialTheme.colorScheme.onSurface,
            currencySymbol = state.currencySymbol,
            currencyPosition = state.currencyPosition,
            decimalSeparator = state.decimalSeparator,
            numberOfDecimals = state.numberOfDecimals,
        )

        WooPosText(
            text = state.changeDueText,
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantLowest,
            modifier = Modifier
                .constrainAs(changeDue) {
                    top.linkTo(input.bottom, margin = smallMargin)
                    start.linkTo(parent.start, margin = standardMargin)
                    end.linkTo(parent.end, margin = standardMargin)
                }
        )

        if (state.errorMessage != null) {
            WooPosText(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = WooPosTypography.BodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(error) {
                    top.linkTo(changeDue.bottom, margin = smallMargin)
                    start.linkTo(parent.start, margin = standardMargin)
                    end.linkTo(parent.end, margin = standardMargin)
                }
            )
        }

        WooPosButton(
            text = state.button.text,
            onClick = onCompleteOrderClicked,
            state = when (state.button.status) {
                WooPosCashPaymentState.Collecting.Button.Status.ENABLED -> WooPosButtonState.ENABLED
                WooPosCashPaymentState.Collecting.Button.Status.DISABLED -> WooPosButtonState.DISABLED
                WooPosCashPaymentState.Collecting.Button.Status.LOADING -> WooPosButtonState.LOADING
            },
            modifier = Modifier
                .constrainAs(button) {
                    bottom.linkTo(parent.bottom, margin = WooPosSpacing.Medium.value)
                    end.linkTo(parent.end, margin = standardMargin)
                    start.linkTo(parent.start, margin = standardMargin)
                    width = Dimension.fillToConstraints
                }
        )
    }
}

@WooPosPreview
@Composable
fun WooPosTotalsPaymentCashScreenPreview() {
    WooPosTheme {
        WooPosCashPaymentScreen(
            state = WooPosCashPaymentState.Collecting(
                enteredAmount = BigDecimal(100),
                errorMessage = null,
                changeDueText = "5$",
                total = BigDecimal(10),
                totalText = "10$",
                currencySymbol = "$",
                currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
                button = WooPosCashPaymentState.Collecting.Button(
                    text = "Mark order as complete",
                    status = WooPosCashPaymentState.Collecting.Button.Status.DISABLED
                )
            ),
            onAmountChanged = {},
            onCompleteOrderClicked = {},
            onBackClicked = {},
            onOrderComplete = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosTotalsPaymentCashWithLabelScreenPreview() {
    WooPosTheme {
        WooPosCashPaymentScreen(
            state = WooPosCashPaymentState.Collecting(
                enteredAmount = null,
                errorMessage = null,
                changeDueText = "Change Due 5$",
                total = BigDecimal(10),
                totalText = "Total: 10$",
                currencySymbol = "$",
                currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
                button = WooPosCashPaymentState.Collecting.Button(
                    text = "Mark order as complete",
                    status = WooPosCashPaymentState.Collecting.Button.Status.LOADING
                )
            ),
            onAmountChanged = {},
            onCompleteOrderClicked = {},
            onBackClicked = {},
            onOrderComplete = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosTotalsPaymentCashWithErrorScreenPreview() {
    WooPosTheme {
        WooPosCashPaymentScreen(
            state = WooPosCashPaymentState.Collecting(
                enteredAmount = BigDecimal(500),
                errorMessage = "Amount must be more or equal to total",
                changeDueText = "Change Due 5$",
                total = BigDecimal(10),
                totalText = "Total: 10$",
                currencySymbol = "$",
                currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
                button = WooPosCashPaymentState.Collecting.Button(
                    text = "Mark order as complete",
                    status = WooPosCashPaymentState.Collecting.Button.Status.ENABLED
                )
            ),
            onAmountChanged = {},
            onCompleteOrderClicked = {},
            onBackClicked = {},
            onOrderComplete = {},
        )
    }
}
