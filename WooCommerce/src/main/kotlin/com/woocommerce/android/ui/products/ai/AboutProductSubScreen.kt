package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField

@Composable
fun AboutProductSubScreen(viewModel: AboutProductSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        Column(
            modifier = modifier.background(MaterialTheme.colors.surface),
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
                    onValueChange = viewModel::onProductFeaturesUpdated,
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
            Text(
                text = stringResource(id = R.string.product_creation_ai_about_product_set_tone),
                style = MaterialTheme.typography.body1,
                color = colorResource(id = R.color.color_primary),
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = R.dimen.minor_100))
            )
        }
    }
}
