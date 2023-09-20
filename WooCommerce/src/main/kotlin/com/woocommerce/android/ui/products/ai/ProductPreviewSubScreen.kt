package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProductPreviewSubScreen(viewModel: ProductPreviewSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        ProductPreviewSubScreen(state, modifier)
    }
}

@Composable
private fun ProductPreviewSubScreen(state: ProductPreviewSubViewModel.State, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_title),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_subtitle),
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
        when (state) {
            ProductPreviewSubViewModel.State.Loading -> ProductPreviewLoading(Modifier.weight(1f))
            is ProductPreviewSubViewModel.State.Success -> TODO()
        }
    }
}

@Composable
private fun ProductPreviewLoading(modifier: Modifier) {
    @Composable
    fun LoadingSkeleton(lines: Int = 2, modifier: Modifier) {
        require(lines == 2 || lines == 3)
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            modifier = modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            if (lines == 3) {
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(dimensionResource(id = R.dimen.major_100))
                )
            }
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(if (lines == 3) 0.5f else 0.6f)
                    .height(dimensionResource(id = R.dimen.major_100))
            )
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(if (lines == 3) 0.7f else 0.8f)
                    .height(dimensionResource(id = R.dimen.major_100))
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        modifier = modifier
    ) {
        val sectionsBackgroundModifier = Modifier.background(
            color = colorResource(id = R.color.woo_gray_6),
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
        )

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_name_section),
            style = MaterialTheme.typography.subtitle1
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBackgroundModifier)
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_description_section),
            style = MaterialTheme.typography.subtitle1
        )
        LoadingSkeleton(
            lines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBackgroundModifier)
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_details_section),
            style = MaterialTheme.typography.subtitle1
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBackgroundModifier)
        )
        Spacer(Modifier)
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBackgroundModifier)
        )
    }
}

@Composable
@Preview
private fun ProductPreviewLoadingPreview() {
    WooThemeWithBackground {
        ProductPreviewSubScreen(ProductPreviewSubViewModel.State.Loading, Modifier.fillMaxSize())
    }
}
