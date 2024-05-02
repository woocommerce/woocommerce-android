package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.LearnMoreAboutSection
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryViewModel.UiState

@Composable
fun TapToPaySummaryScreen(viewModel: TapToPaySummaryViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        TapToPaySummaryScreen(
            uiState = state,
            onTryPaymentClicked = viewModel::onTryPaymentClicked,
            onBackClick = viewModel::onBackClicked,
            onLearnMoreClicked = viewModel::onLearnMoreClicked,
        )
    }
}

@Composable
fun TapToPaySummaryScreen(
    uiState: UiState,
    onTryPaymentClicked: () -> Unit,
    onBackClick: () -> Unit,
    onLearnMoreClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.card_reader_tap_to_pay_explanation_screen_title),
                onNavigationButtonClick = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_200)),
                text = stringResource(id = R.string.card_reader_tap_to_pay_explanation_title),
                style = MaterialTheme.typography.h5,
                textAlign = Center,
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Image(
                painter = painterResource(id = R.drawable.img_tap_to_pay_summary),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

            Column(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                horizontalAlignment = CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_explanation_ready),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = Center,
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    text = stringResource(id = R.string.card_reader_tap_to_pay_explanation_easy),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = Center,
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
                Text(
                    text = uiState.messageWithAmount,
                    style = MaterialTheme.typography.body1,
                    textAlign = Center,
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

            Column(horizontalAlignment = CenterHorizontally) {
                Text(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                    text = stringResource(id = R.string.card_reader_tap_to_pay_explanation_where_to_find),
                    style = MaterialTheme.typography.caption,
                    textAlign = Center,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                WCColoredButton(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                        .fillMaxWidth(),
                    onClick = onTryPaymentClicked,
                    enabled = !uiState.isProgressVisible
                ) {
                    if (uiState.isProgressVisible) {
                        CircularProgressIndicator(
                            modifier = Modifier.then(Modifier.size(dimensionResource(id = R.dimen.major_100))),
                        )
                    } else {
                        Text(stringResource(id = R.string.card_reader_tap_to_pay_explanation_try_payment))
                    }
                }

                LearnMoreAboutSection(
                    textWithUrl = R.string.card_reader_tap_to_pay_learn_more,
                    onClick = onLearnMoreClicked,
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        }
    }
}

@PreviewLightDark
@Composable
fun TapToPaySummaryScreenPreview() {
    WooThemeWithBackground {
        TapToPaySummaryScreen(
            uiState = UiState(
                isProgressVisible = false,
                messageWithAmount = "Try a $0.50 payment with your debit or credit card."
            ),
            onTryPaymentClicked = {},
            onBackClick = {},
            onLearnMoreClicked = {},
        )
    }
}
