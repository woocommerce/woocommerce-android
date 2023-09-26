package com.woocommerce.android.ui.login.storecreation.summary

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
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
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.ui.compose.autoMirror
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreCreationSummaryScreen(viewModel: StoreCreationSummaryViewModel) {
    viewModel.isLoading.observeAsState().value?.let { isLoading ->
        StoreCreationSummaryScreen(
            modifier = Modifier,
            onCancelPressed = viewModel::onCancelPressed,
            onTryForFreeButtonPressed = viewModel::onTryForFreeButtonPressed,
            isLoading = isLoading
        )
    }
}

@Composable
private fun StoreCreationSummaryScreen(
    modifier: Modifier,
    onCancelPressed: () -> Unit,
    onTryForFreeButtonPressed: () -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = drawable.store_free_trial_summary_top_background),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = dimensionResource(id = dimen.major_150))
                .scale(scaleX = if (isSystemInRTL()) -1f else 1f, scaleY = 1f)
        )
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onCancelPressed) {
                Icon(
                    imageVector = Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                    modifier = Modifier.autoMirror()
                )
            }
            SummaryBody(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(5f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(id = dimen.major_125))
                    .padding(
                        top = dimensionResource(id = dimen.major_300),
                        bottom = dimensionResource(id = dimen.major_75)
                    )
            )
            SummaryBottom(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onTryForFreeButtonPressed = onTryForFreeButtonPressed,
                isLoading = isLoading
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
        Text(
            text = stringResource(id = R.string.free_trial_summary_start_selling_quickly),
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.minor_100))
        )
        FeatureRow(stringId = R.string.free_trial_feature_premium_themes)
        FeatureRow(stringId = R.string.free_trial_feature_store_management)
        FeatureRow(stringId = R.string.free_trial_feature_design_editing)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

        Text(
            text = stringResource(id = R.string.free_trial_summary_grow_your_business),
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.minor_100))
        )
        FeatureRow(stringId = R.string.free_trial_feature_built_in_seo)
        FeatureRow(stringId = R.string.free_trial_feature_social_advertising)
        FeatureRow(stringId = R.string.free_trial_feature_automated_customer_emails)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

        Text(
            text = stringResource(id = R.string.free_trial_summary_hassle_free_store_ownership),
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.minor_100))
        )
        FeatureRow(stringId = R.string.free_trial_feature_hosting)
        FeatureRow(stringId = R.string.free_trial_feature_backups)
        FeatureRow(stringId = R.string.free_trial_feature_support)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
    }
}

@Composable
private fun FeatureRow(@StringRes stringId: Int) {
    Row(
        modifier = Modifier
            .padding(bottom = dimensionResource(id = R.dimen.major_75))
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_free_trial_item_check),
            contentDescription = null,
            tint = colorResource(id = R.color.color_primary),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = dimensionResource(id = R.dimen.minor_100))
                .size(dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(id = stringId),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Start,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun SummaryBottom(
    modifier: Modifier,
    onTryForFreeButtonPressed: () -> Unit,
    isLoading: Boolean
) {
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
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)),
                    color = colorResource(id = R.color.color_on_primary_surface),
                )
            } else {
                Text(stringResource(id = R.string.free_trial_summary_try_button))
            }
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
            onTryForFreeButtonPressed = {},
            isLoading = false
        )
    }
}

@Preview(name = "Loading mode")
@Composable
fun StoreCreationSummaryLoading() {
    WooThemeWithBackground {
        StoreCreationSummaryScreen(
            modifier = Modifier,
            onCancelPressed = {},
            onTryForFreeButtonPressed = {},
            isLoading = true
        )
    }
}
