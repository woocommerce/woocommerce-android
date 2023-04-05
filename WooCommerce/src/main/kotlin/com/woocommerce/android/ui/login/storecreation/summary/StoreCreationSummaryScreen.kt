package com.woocommerce.android.ui.login.storecreation.summary

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                .padding(it)
                .fillMaxSize()
        ) {
            SummaryBody(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_125))
                    .weight(4f)
            )
            SummaryBottom(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                end = dimensionResource(id = R.dimen.major_400),
                bottom = dimensionResource(id = R.dimen.major_100)
            )
        )
        Text(
            text = stringResource(id = R.string.free_trial_summary_advertise),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(
                bottom = dimensionResource(id = R.dimen.major_200)
            )
        )
        Text(
            text = stringResource(id = R.string.free_trial_summary_try_it),
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                bottom = dimensionResource(id = R.dimen.major_100)
            )
        )
        SummaryFreeTrialFeatureList()
    }
}

@Composable
private fun SummaryFreeTrialFeatureList() {
    Column {
        FeatureRow(iconResourceId = R.drawable.ic_star, stringResourceId = R.string.free_trial_feature_premium_themes)
        FeatureRow(iconResourceId = R.drawable.ic_box, stringResourceId = R.string.free_trial_feature_unlimited_products)
        FeatureRow(iconResourceId = R.drawable.ic_truck, stringResourceId = R.string.free_trial_feature_shipping_labels)
        FeatureRow(iconResourceId = R.drawable.ic_chart, stringResourceId = R.string.free_trial_feature_ecommerce_reports)
        FeatureRow(iconResourceId = R.drawable.ic_dollar, stringResourceId = R.string.free_trial_feature_multiple_payment_options)
        FeatureRow(iconResourceId = R.drawable.ic_gift, stringResourceId = R.string.free_trial_feature_subscription_product_kits)
        FeatureRow(iconResourceId = R.drawable.ic_megaphone, stringResourceId = R.string.free_trial_feature_social_advertising)
        FeatureRow(iconResourceId = R.drawable.ic_letter, stringResourceId = R.string.free_trial_feature_email_marketing)
        FeatureRow(iconResourceId = R.drawable.ic_two_persons, stringResourceId = R.string.free_trial_feature_247_support)
        FeatureRow(iconResourceId = R.drawable.ic_update_clock, stringResourceId = R.string.free_trial_feature_auto_updates_backups)
        FeatureRow(iconResourceId = R.drawable.ic_cloud, stringResourceId = R.string.free_trial_feature_site_security)
        FeatureRow(iconResourceId = R.drawable.ic_globe_2, stringResourceId = R.string.free_trial_feature_fast_to_launch)
    }
}

@Composable
private fun FeatureRow(
    iconResourceId: Int,
    stringResourceId: Int
) {
    Row(modifier = Modifier
        .padding(bottom = dimensionResource(id = R.dimen.major_75))
    ) {
        Icon(
            painter = painterResource(id = iconResourceId),
            contentDescription = null,
            tint = colorResource(id = R.color.color_primary),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = dimensionResource(id = R.dimen.minor_100))
                .size(dimensionResource(id = R.dimen.major_150))
        )
        Text(
            text = stringResource(id =stringResourceId),
            textAlign = TextAlign.Start,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun SummaryBottom(
    modifier: Modifier,
    onTryForFreeButtonPressed: () -> Unit
) {
    val primaryPurple = colorResource(id = R.color.color_primary)
    val buttonColors = ButtonDefaults.buttonColors(
        backgroundColor = primaryPurple,
        contentColor = MaterialTheme.colors.onPrimary,
        disabledBackgroundColor = primaryPurple.copy(alpha = 0.38f),
        disabledContentColor = MaterialTheme.colors.onPrimary
    )

    Column(modifier = modifier) {
        Divider()
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_125)),
            onClick = onTryForFreeButtonPressed,
            colors = buttonColors
        ) {
            Text(stringResource(id = R.string.free_trial_summary_try_button))
        }
        Text(
            text = stringResource(id = R.string.free_trial_summary_credit_card_message),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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
