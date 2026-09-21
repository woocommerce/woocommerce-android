package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayIntro
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocalNetworkPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocalNetworkPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayViewState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.util.UiHelpers

@Composable
fun CardReaderModeScreen(viewModel: CardReaderModeViewModel) {
    val state by viewModel.viewState.collectAsState()
    CardReaderModeContent(state)
}

@Composable
private fun CardReaderModeContent(state: RemoteTapToPayViewState?) {
    if (state is RemoteTapToPayIntro) {
        BackHandler(onBack = state.onSecondaryActionClicked)
    }
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        AnimatedContent(
            targetState = state,
            label = "CardReaderModeContent",
            transitionSpec = {
                fadeIn(animationSpec = tween()) togetherWith fadeOut(animationSpec = tween())
            },
            contentKey = { it?.let { it::class.simpleName } ?: "null" },
        ) { targetState ->
            when (targetState) {
                null,
                is RemoteTapToPayStarting -> StartingContent(targetState)
                is RemoteTapToPayIntro,
                is RemoteTapToPayLocationPermissionExplainer,
                is RemoteTapToPayLocationPermissionDenied,
                is RemoteTapToPayLocalNetworkPermissionExplainer,
                is RemoteTapToPayLocalNetworkPermissionDenied,
                is RemoteTapToPayReadyToPair,
                is RemoteTapToPayWaitingForPayment,
                is RemoteTapToPayError -> StatefulContent(targetState)
            }
        }
    }
}

@Composable
private fun StartingContent(state: RemoteTapToPayViewState?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE_DP),
                strokeWidth = SPINNER_STROKE_DP,
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                text = stringResource(
                    id = state?.headerLabel ?: R.string.card_reader_mode_starting_header
                ),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            state?.paymentStateLabel?.let { subtitle ->
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = UiHelpers.getTextOfUiString(LocalContext.current, subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun illustrationModifier(state: RemoteTapToPayViewState): Modifier = when (state) {
    is RemoteTapToPayIntro -> Modifier
    is RemoteTapToPayStarting,
    is RemoteTapToPayLocationPermissionExplainer,
    is RemoteTapToPayLocationPermissionDenied,
    is RemoteTapToPayLocalNetworkPermissionExplainer,
    is RemoteTapToPayLocalNetworkPermissionDenied,
    is RemoteTapToPayReadyToPair,
    is RemoteTapToPayWaitingForPayment,
    is RemoteTapToPayError -> Modifier.size(dimensionResource(id = R.dimen.image_major_120))
}

@Composable
private fun StatefulContent(state: RemoteTapToPayViewState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            state.illustration?.let { drawable ->
                Image(
                    painter = painterResource(id = drawable),
                    contentDescription = null,
                    modifier = illustrationModifier(state),
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            state.headerLabel?.let { header ->
                Text(
                    text = stringResource(id = header),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            state.paymentStateLabel?.let { subtitle ->
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = UiHelpers.getTextOfUiString(LocalContext.current, subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            when (state) {
                is RemoteTapToPayIntro -> IntroRequirements()
                is RemoteTapToPayReadyToPair -> PairingDetails(state)
                is RemoteTapToPayStarting,
                is RemoteTapToPayLocationPermissionExplainer,
                is RemoteTapToPayLocationPermissionDenied,
                is RemoteTapToPayLocalNetworkPermissionExplainer,
                is RemoteTapToPayLocalNetworkPermissionDenied,
                is RemoteTapToPayWaitingForPayment,
                is RemoteTapToPayError -> Unit
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Column(modifier = Modifier.fillMaxWidth()) {
            state.primaryActionLabel?.let { labelRes ->
                WCColoredButton(
                    onClick = { state.onPrimaryActionClicked?.invoke() },
                    text = stringResource(id = labelRes),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.secondaryActionLabel?.let { labelRes ->
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                WCOutlinedButton(
                    onClick = { state.onSecondaryActionClicked?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = labelRes))
                }
            }
        }
    }
}

@Composable
private fun PairingDetails(state: RemoteTapToPayReadyToPair) {
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    Text(
        text = stringResource(
            id = R.string.card_reader_mode_device_identifier,
            state.deviceName,
            state.fingerprintSuffix,
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    state.siteUrl?.takeIf { it.isNotBlank() }?.let { url ->
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(id = R.string.card_reader_mode_ready_to_pair_store, url),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IntroRequirements() {
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.card_reader_mode_intro_requirements_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        IntroRequirement(text = stringResource(id = R.string.card_reader_mode_intro_requirement_pos))
        IntroRequirement(text = stringResource(id = R.string.card_reader_mode_intro_requirement_wifi))
        IntroRequirement(text = stringResource(id = R.string.card_reader_mode_intro_requirement_nfc))
    }
}

@Composable
private fun IntroRequirement(text: String) {
    Row(modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_50))) {
        Text(
            text = BULLET,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.minor_100)),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val SPINNER_SIZE_DP = 72.dp
private val SPINNER_STROKE_DP = 6.dp
private const val BULLET = "\u2022"

@PreviewLightDark
@Composable
fun CardReaderModeIntroPreview() {
    WooThemeWithBackground {
        CardReaderModeContent(
            state = RemoteTapToPayIntro(
                onPrimaryActionClicked = {},
                onSecondaryActionClicked = {},
            )
        )
    }
}

@PreviewLightDark
@Composable
fun CardReaderModeStartingPreview() {
    WooThemeWithBackground {
        CardReaderModeContent(state = RemoteTapToPayStarting(onPrimaryActionClicked = {}))
    }
}

@PreviewLightDark
@Composable
fun CardReaderModeReadyToPairPreview() {
    WooThemeWithBackground {
        CardReaderModeContent(
            state = RemoteTapToPayReadyToPair(
                deviceName = "Pixel 7",
                fingerprintSuffix = "AB4F",
                siteUrl = "example.com",
                onPrimaryActionClicked = {},
            )
        )
    }
}

@PreviewLightDark
@Composable
fun CardReaderModeWaitingForPaymentPreview() {
    WooThemeWithBackground {
        CardReaderModeContent(
            state = RemoteTapToPayWaitingForPayment(
                tabletName = "iPad Pro",
                onPrimaryActionClicked = {},
            )
        )
    }
}

@PreviewLightDark
@Composable
fun CardReaderModeErrorPreview() {
    WooThemeWithBackground {
        CardReaderModeContent(
            state = RemoteTapToPayError(
                message = "java.net.SocketTimeoutException: Connection timed out",
                onPrimaryActionClicked = {},
            )
        )
    }
}
