package com.woocommerce.android.ui.readermode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.util.UiHelpers

@Composable
fun CardReaderModeScreen(viewModel: CardReaderModeViewModel) {
    val state by viewModel.stateOverride.observeAsState()
    CardReaderModeContent(state)
}

@Composable
private fun CardReaderModeContent(state: ViewState?) {
    if (state == null) return
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.illustration?.let { drawable ->
                Image(
                    painter = painterResource(id = drawable),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.image_major_120)),
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            state.headerLabel?.let { header ->
                Text(
                    text = stringResource(id = header),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
            state.paymentStateLabel?.let { subtitle ->
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = UiHelpers.getTextOfUiString(LocalContext.current, subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        state.primaryActionLabel?.let { labelRes ->
            Button(
                onClick = { state.onPrimaryActionClicked?.invoke() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = labelRes))
            }
        }
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
