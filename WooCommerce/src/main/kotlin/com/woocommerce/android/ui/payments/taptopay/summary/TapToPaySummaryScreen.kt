package com.woocommerce.android.ui.payments.taptopay.summary

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryViewModel.UiState

@Composable
fun TapToPaySummaryScreen(viewModel: TapToPaySummaryViewModel) {
    viewModel.uiState.observeAsState().value?.let { state ->
        TapToPaySummaryScreen(
            state,
            onTryPaymentClicked = viewModel::onTryPaymentClicked,
            onLearnMoreClicked = viewModel::onLearnMoreClicked,
            onBackClick = viewModel::onBackClicked
        )
    }
}

@Composable
fun TapToPaySummaryScreen(
    state: UiState,
    onTryPaymentClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onBackClick: () -> Unit,
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
                .fillMaxSize(),
            horizontalAlignment = CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                text = stringResource(id = state.titleText),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1.0f))
            Image(
                painter = painterResource(id = state.illustration),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.weight(1.0f))

            ExplanationRaw("1", stringResource(id = state.explanationOneText))
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            ExplanationRaw("2", stringResource(id = state.explanationTwoText))
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            ExplanationRaw("3", stringResource(id = state.explanationThreeText))

            Spacer(modifier = Modifier.weight(1.0f))

            WCColoredButton(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth(),
                onClick = onTryPaymentClicked
            ) {
                Text(stringResource(id = state.buttonText))
            }

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            LearnMore(state.learnMoreText, onLearnMoreClicked)
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
        }
    }
}

@Composable
private fun ExplanationRaw(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = CenterVertically,
    ) {
        val numberBackgroundColor = colorResource(id = R.color.disconnected_state_number_background_color)
        val radius = dimensionResource(id = R.dimen.major_100)
        Text(
            modifier = Modifier
                .drawBehind {
                    drawCircle(color = numberBackgroundColor, radius = radius.toPx())
                }
                .padding(16.dp),
            text = number,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
        )
    }
}

@Composable
private fun LearnMore(@StringRes textId: Int, onLearnMoreClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            ) { onLearnMoreClicked() }
            .padding(
                vertical = dimensionResource(id = R.dimen.minor_100),
                horizontal = dimensionResource(id = R.dimen.major_125)
            ),
        verticalAlignment = CenterVertically,
    ) {
        Image(painter = painterResource(id = R.drawable.ic_info_outline_20dp), contentDescription = null)

        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))

        Text(
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
            text = annotatedStringRes(stringResId = textId),
            style = MaterialTheme.typography.caption.copy(
                color = colorResource(id = R.color.color_on_surface_medium)
            ),
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small device", device = Devices.PIXEL)
@Composable
fun TapToPaySummaryScreenPreview() {
    TapToPaySummaryScreen(
        state = UiState,
        onTryPaymentClicked = {},
        onLearnMoreClicked = {},
        onBackClick = {}
    )
}
