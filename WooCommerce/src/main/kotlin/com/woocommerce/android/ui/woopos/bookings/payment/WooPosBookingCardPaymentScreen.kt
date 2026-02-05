package com.woocommerce.android.ui.woopos.bookings.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosBookingCardPaymentScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    viewModel: WooPosBookingCardPaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        when (state) {
            is BookingCardPaymentState.PaymentFailed -> {
                onNavigationEvent(WooPosNavigationEvent.GoBack)
            }
            else -> viewModel.onCancelClicked()
        }
    }

    when (val currentState = state) {
        is BookingCardPaymentState.Loading -> {
            BookingCardPaymentLoadingContent()
        }
        is BookingCardPaymentState.CollectingPayment -> {
            BookingCardPaymentCollectingContent(
                amountLabel = currentState.amountLabel,
                onCancel = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
            )
        }
        is BookingCardPaymentState.ProcessingPayment -> {
            BookingCardPaymentProcessingContent(
                amountLabel = currentState.amountLabel,
            )
        }
        is BookingCardPaymentState.PaymentSuccessful -> {
            LaunchedEffect(Unit) {
                onNavigationEvent(
                    WooPosNavigationEvent.OpenBookingPaymentSuccess(
                        orderId = currentState.orderId,
                        amountLabel = currentState.amountLabel,
                    )
                )
            }
        }
        is BookingCardPaymentState.PaymentFailed -> {
            BookingCardPaymentFailedContent(
                errorMessage = currentState.errorMessage,
                onRetry = { viewModel.onRetryClicked() },
                onCancel = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
            )
        }
    }
}

@Composable
private fun BookingCardPaymentLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BookingCardPaymentCollectingContent(
    amountLabel: String,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration)
            )
            LottieAnimation(
                modifier = Modifier.size(256.dp),
                composition = composition,
                iterations = LottieConstants.IterateForever,
                clipToCompositionBounds = false,
                clipSpec = LottieClipSpec.Markers(
                    "payment_processing_start",
                    "payment_processing_end"
                ),
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            WooPosText(
                text = stringResource(R.string.woopos_bookings_card_payment_tap_insert_swipe),
                color = MaterialTheme.colorScheme.onPrimary,
                style = WooPosTypography.BodyLarge,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            WooPosText(
                text = amountLabel,
                color = MaterialTheme.colorScheme.onPrimary,
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
            WooPosOutlinedButton(
                modifier = Modifier
                    .height(80.dp)
                    .width(604.dp),
                text = stringResource(R.string.cancel),
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun BookingCardPaymentProcessingContent(
    amountLabel: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration)
            )
            LottieAnimation(
                modifier = Modifier.size(256.dp),
                composition = composition,
                iterations = LottieConstants.IterateForever,
                clipToCompositionBounds = false,
                clipSpec = LottieClipSpec.Markers(
                    "payment_processing_start",
                    "payment_processing_end"
                ),
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            WooPosText(
                text = stringResource(R.string.woopos_bookings_card_payment_processing),
                color = MaterialTheme.colorScheme.onPrimary,
                style = WooPosTypography.BodyLarge,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            WooPosText(
                text = amountLabel,
                color = MaterialTheme.colorScheme.onPrimary,
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BookingCardPaymentFailedContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(84.dp),
            imageVector = WooPosIcons.ErrorX,
            contentDescription = stringResource(R.string.woopos_error_icon_content_description),
            tint = WooPosTheme.colors.unspecified,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosText(
            text = stringResource(R.string.woopos_bookings_card_payment_failed),
            style = WooPosTypography.BodyXLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosText(
            text = errorMessage,
            style = WooPosTypography.BodyLarge,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosButton(
            text = stringResource(R.string.woopos_bookings_card_payment_try_again),
            modifier = Modifier
                .height(80.dp)
                .width(604.dp),
            onClick = onRetry,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
        WooPosOutlinedButton(
            modifier = Modifier.width(604.dp),
            text = stringResource(R.string.cancel),
            onClick = onCancel,
        )
    }
}
