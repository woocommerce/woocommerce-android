package com.woocommerce.android.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProductAIToolsScreen(viewModel: ProductAIToolsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        ProductAIToolsScreen(viewState = it)
    }
}

@Composable
fun ProductAIToolsScreen(
    viewState: ProductAIToolsViewModel.ViewState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(vertical = dimensionResource(id = R.dimen.major_100)),
    ) {
        items(viewState.options.size) { index ->
            val option = viewState.options[index]
            ProductAIToolOption(
                title = option.title,
                description = option.description,
                onClick = option.onClick
            )
        }

    }
}

@Composable
fun ProductAIToolOption(
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            text = title,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.margin_extra_small)))
        Text(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            text = description,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface
        )
        Divider(
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.major_100)),
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
    }

}

@Preview
@Composable
private fun ProductsAIToolsScreenPreview() {
    WooThemeWithBackground {
        ProductAIToolsScreen(
            viewState = ProductAIToolsViewModel.ViewState(
                options = listOf(
                    ProductAIToolsViewModel.AIToolOption(
                        title = stringResource(R.string.ai_product_tools_generate_tweet),
                        description = stringResource(R.string.ai_product_tools_generate_tweet_description),
                    ),
                    ProductAIToolsViewModel.AIToolOption(
                        title = stringResource(R.string.ai_product_tools_generate_tweet),
                        description = stringResource(R.string.ai_product_tools_generate_tweet_description),
                    ),
                    ProductAIToolsViewModel.AIToolOption(
                        title = stringResource(R.string.ai_product_tools_generate_tweet),
                        description = stringResource(R.string.ai_product_tools_generate_tweet_description),
                    ),
                )
            )
        )
    }
}
