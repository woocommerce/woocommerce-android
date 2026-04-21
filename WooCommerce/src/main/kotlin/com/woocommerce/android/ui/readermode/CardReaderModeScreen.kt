package com.woocommerce.android.ui.readermode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.util.UiHelpers

@Composable
fun CardReaderModeScreen(viewModel: CardReaderModeViewModel) {
    val state by viewModel.stateOverride.observeAsState()
    CardReaderModeContent(state)
}

@Composable
private fun CardReaderModeContent(state: ViewState?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state == null) return@Surface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING_DP),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.size(TOP_SPACER_DP))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.illustration?.let { drawable ->
                    Image(
                        painter = painterResource(id = drawable),
                        contentDescription = null,
                        modifier = Modifier.size(ILLUSTRATION_SIZE_DP),
                    )
                }
                Box(modifier = Modifier.size(SPACER_DP))
                state.headerLabel?.let { header ->
                    Text(
                        text = stringResource(id = header),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                state.paymentStateLabel?.let { subtitle ->
                    val context = LocalContext.current
                    Box(modifier = Modifier.size(SMALL_SPACER_DP))
                    Text(
                        text = UiHelpers.getTextOfUiString(context, subtitle),
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
}

private val SCREEN_PADDING_DP = 24.dp
private val TOP_SPACER_DP = 48.dp
private val ILLUSTRATION_SIZE_DP = 160.dp
private val SPACER_DP = 24.dp
private val SMALL_SPACER_DP = 8.dp
