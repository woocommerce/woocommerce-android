package com.woocommerce.android.ui.login.storecreation.summary

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreCreationSummaryScreen(viewModel: StoreCreationSummaryViewModel) {
    StoreCreationSummaryScreen(
        modifier = Modifier,
        onCancelPressed = viewModel::onCancelPressed,
        onTryForFreeButtonPressed = viewModel::onTryForFreeButtonPressed
    )
}

@Composable
private fun StoreCreationSummaryScreen(
    modifier: Modifier,
    onCancelPressed: () -> Unit,
    onTryForFreeButtonPressed: () -> Unit
) {
    Scaffold(topBar = {
        Toolbar(onNavigationButtonClick = onCancelPressed)
    }) {
        Column(
            modifier = modifier
                .padding(it)
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(5f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.free_trial_summary_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = dimensionResource(id = R.dimen.major_150))
                        .scale(scaleX = if (isSystemInRTL()) -1f else 1f, scaleY = 1f)
                )

                SummaryBody(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.major_125))
                        .padding(
                            top = dimensionResource(id = R.dimen.free_trial_summary_title_top_padding),
                            bottom = dimensionResource(id = R.dimen.major_75)
                        )
                )
            }
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

        Icon(
            painter = painterResource(id = R.drawable.ic_powered_by),
            contentDescription = null,
            tint = colorResource(id = R.color.color_on_surface_medium),
        )
    }
}

@Composable
private fun SummaryFreeTrialFeatureList() {
    Column {
        FeatureRow(iconId = R.drawable.ic_star, stringId = R.string.free_trial_feature_premium_themes)
        FeatureRow(iconId = R.drawable.ic_box, stringId = R.string.free_trial_feature_unlimited_products)
        FeatureRow(iconId = R.drawable.ic_truck, stringId = R.string.free_trial_feature_shipping_labels)
        FeatureRow(iconId = R.drawable.ic_chart, stringId = R.string.free_trial_feature_ecommerce_reports)
        FeatureRow(iconId = R.drawable.ic_dollar, stringId = R.string.free_trial_feature_multiple_payment_options)
        FeatureRow(iconId = R.drawable.ic_gift, stringId = R.string.free_trial_feature_subscription_product_kits)
        FeatureRow(iconId = R.drawable.ic_megaphone, stringId = R.string.free_trial_feature_social_advertising)
        FeatureRow(iconId = R.drawable.ic_letter, stringId = R.string.free_trial_feature_email_marketing)
        FeatureRow(iconId = R.drawable.ic_two_persons, stringId = R.string.free_trial_feature_247_support)
        FeatureRow(iconId = R.drawable.ic_update_clock, stringId = R.string.free_trial_feature_auto_updates_backups)
        FeatureRow(iconId = R.drawable.ic_cloud, stringId = R.string.free_trial_feature_site_security)
        FeatureRow(iconId = R.drawable.ic_globe_2, stringId = R.string.free_trial_feature_fast_to_launch)
    }
}

@Composable
private fun FeatureRow(
    iconId: Int,
    stringId: Int
) {
    Row(
        modifier = Modifier
            .padding(bottom = dimensionResource(id = R.dimen.major_75))
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null,
            tint = colorResource(id = R.color.color_primary),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = dimensionResource(id = R.dimen.minor_100))
                .size(dimensionResource(id = R.dimen.major_150))
        )
        Text(
            text = stringResource(id = stringId),
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
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_125),
                    vertical = dimensionResource(id = R.dimen.minor_100)
                ),
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

@Composable
fun isSystemInRTL(): Boolean {
    return LocalLayoutDirection.current == LayoutDirection.Rtl
}

@Preview(name = "Light mode", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small device", device = Devices.PIXEL)
@Preview(name = "RTL mode", locale = "ar")
@Composable
fun StoreCreationSummary() {
    WooThemeWithBackground {
        StoreCreationSummaryScreen(
            modifier = Modifier,
            onCancelPressed = {},
            onTryForFreeButtonPressed = {}
        )
    }
}
