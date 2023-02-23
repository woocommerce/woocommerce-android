package com.woocommerce.android.ui.payments.taptopay.summary

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryViewModel.UiState

@Composable
fun TapToPaySummaryScreen(viewModel: TapToPaySummaryViewModel) {
    viewModel.uiState.observeAsState().value?.let { state ->
        TapToPaySummaryScreen(
            state,
            onTryPaymentClicked = viewModel::onTryPaymentClicked,
            onBackClick = viewModel::onBackClicked
        )
    }
}

@Composable
fun TapToPaySummaryScreen(
    state: UiState,
    onTryPaymentClicked: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = state.screenTitleText),
                onNavigationButtonClick = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_200)))
            Text(
                text = stringResource(id = state.titleText),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_200)))
            Image(
                painter = painterResource(id = state.illustration),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_200)))

            WCColoredButton(onClick = onTryPaymentClicked) {

            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TapToPaySummaryScreenPreview() {
    TapToPaySummaryScreen(
        state = UiState,
        onTryPaymentClicked = {},
        onBackClick = {}
    )
}
