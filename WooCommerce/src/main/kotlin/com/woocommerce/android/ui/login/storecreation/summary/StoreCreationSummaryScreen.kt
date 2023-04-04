package com.woocommerce.android.ui.login.storecreation.summary

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreCreationSummaryScreen(viewModel: StoreCreationSummaryViewModel) {
    StoreCreationSummaryScreen(
        onCancelPressed = viewModel::onCancelPressed,
        onHelpPressed = viewModel::onHelpPressed,
        onTryForFreeButtonPressed = viewModel::onTryForFreeButtonPressed
    )
}

@Composable
private fun StoreCreationSummaryScreen(
    onCancelPressed: () -> Unit,
    onHelpPressed: () -> Unit,
    onTryForFreeButtonPressed: () -> Unit
) {
    Scaffold(topBar = {
        ToolbarWithHelpButton(
            onNavigationButtonClick = onCancelPressed,
            onHelpButtonClick = onHelpPressed,
        )
    }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            SummaryBody(modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_125))
                .weight(4f)
            )
            SummaryBottom(
                modifier = Modifier.weight(1f),
                onTryForFreeButtonPressed = onTryForFreeButtonPressed
            )
        }
    }
}

@Composable
private fun SummaryBody(modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.free_trial_summary_title),
            style = MaterialTheme.typography.h4
        )
        Text(
            text = stringResource(id = R.string.free_trial_summary_advertise),
            style = MaterialTheme.typography.body1
        )
        Text(
            text = stringResource(id = R.string.free_trial_summary_try_it),
            style = MaterialTheme.typography.h5
        )
    }
}

@Composable
private fun SummaryBottom(
    modifier: Modifier,
    onTryForFreeButtonPressed: () -> Unit
) {
    Column(modifier = modifier) {
        Divider()
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_125)),
            onClick = onTryForFreeButtonPressed
        ) {
            Text(stringResource(id = R.string.free_trial_summary_try_button))
        }
        Text(
            text = stringResource(id = R.string.free_trial_summary_credit_card_message),
            style = MaterialTheme.typography.caption
        )
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun StoreCreationSummary() {
    WooThemeWithBackground {
        StoreCreationSummaryScreen(
            onCancelPressed = {},
            onHelpPressed = {},
            onTryForFreeButtonPressed = {}
        )
    }
}
