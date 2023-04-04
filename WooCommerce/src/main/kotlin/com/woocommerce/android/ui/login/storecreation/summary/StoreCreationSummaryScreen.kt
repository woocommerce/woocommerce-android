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
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
                .padding(it)
                .padding(dimensionResource(id = R.dimen.major_125))
        ) {
            Text(
                text = "Launch in days, grow for years",
                style = MaterialTheme.typography.h4
            )
            Text(
                "We offer everything you need to build and grow an online store, " +
                    "powered by WooCommerce and hosted on WordPress.com.",
                style = MaterialTheme.typography.body1
            )
            Text(
                "Try it for 14 days.",
                style = MaterialTheme.typography.h5
            )
            Divider()
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onTryForFreeButtonPressed
            ) {
                Text("Try for free")
            }
            Text(
                "No credit card required.",
                style = MaterialTheme.typography.caption
            )
        }
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
