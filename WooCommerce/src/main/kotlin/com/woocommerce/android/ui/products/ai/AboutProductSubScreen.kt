package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.UiState

@Composable
fun AboutProductSubScreen(viewModel: AboutProductSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        AboutProductSubScreen(
            state,
            viewModel::onProductFeaturesUpdated,
            viewModel::onDoneClick,
            viewModel::onChangeToneClicked,
            modifier
        )
    }
}

@Composable
fun AboutProductSubScreen(
    state: UiState,
    onProductFeaturesUpdated: (String) -> Unit,
    onCreateProductDetails: () -> Unit,
    onChangeTone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colors.surface)) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.product_creation_ai_about_product_title),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.product_creation_ai_about_product_subtitle),
                style = MaterialTheme.typography.subtitle1
            )
            Column(
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_150)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
            ) {
                Text(
                    text = stringResource(id = R.string.product_creation_ai_about_product_edit_text_header),
                    style = MaterialTheme.typography.subtitle2,
                )
                WCOutlinedTextField(
                    value = state.productFeatures,
                    onValueChange = onProductFeaturesUpdated,
                    label = stringResource(id = R.string.product_creation_ai_about_product_edit_text_hint),
                    textFieldModifier = Modifier.height(dimensionResource(id = R.dimen.major_400))
                )
                Text(
                    text = stringResource(id = R.string.product_creation_ai_about_product_edit_text_caption),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = R.color.color_on_surface_medium),
                )
            }
            Text(
                text = stringResource(id = R.string.product_creation_ai_about_product_edit_text_caption),
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium),
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = R.dimen.minor_100))
            )
            WCTextButton(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = R.dimen.minor_100)),
                contentPadding = PaddingValues(dimensionResource(id = dimen.major_100)),
                onClick = onChangeTone
            ) {
                Text(
                    text = stringResource(id = R.string.product_creation_ai_about_product_set_tone),
                    style = MaterialTheme.typography.body1,
                    color = colorResource(id = R.color.color_primary)
                )
            }
        }
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreateProductDetails,
            enabled = state.productFeatures.isNotBlank()
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun AboutProductSubScreenPreview() {
    AboutProductSubScreen(
        state = UiState(
            productFeatures = "productFeatures"
        ),
        onProductFeaturesUpdated = {},
        onCreateProductDetails = {},
        onChangeTone = {},
        modifier = Modifier
    )
}
