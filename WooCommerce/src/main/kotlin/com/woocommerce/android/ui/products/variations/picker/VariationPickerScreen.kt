package com.woocommerce.android.ui.products.variations.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.variations.selector.EmptyVariationList
import com.woocommerce.android.ui.products.variations.selector.VariationListSkeleton

@Composable
fun VariationPickerScreen(viewModel: VariationPickerViewModel) {
    val viewState by viewModel.viewSate.observeAsState(VariationPickerViewModel.ViewState())
    VariationPickerScreen(
        viewState,
        viewModel::onSelectVariation,
        viewModel::onLoadMore,
        viewModel::onCancel
    )
}

@Composable
fun VariationPickerScreen(
    state: VariationPickerViewModel.ViewState,
    onVariationClick: (variation: VariationPickerViewModel.VariationListItem) -> Unit,
    onLoadMore: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(id = R.string.product_variation_picker_title)) },
            navigationIcon = {
                IconButton(onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.close)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.color_toolbar),
            elevation = 0.dp,
        )
    }) { padding ->
        when {
            state.variations.isNotEmpty() -> VariationList(
                state = state,
                onVariationClick = onVariationClick,
                onLoadMore = onLoadMore,
                modifier = Modifier.padding(padding)
            )

            state.loadingState == VariationPickerViewModel.LoadingState.LOADING -> VariationListSkeleton()
            else -> EmptyVariationList()
        }
    }
}

@Composable
private fun VariationList(
    state: VariationPickerViewModel.ViewState,
    onVariationClick: (variation: VariationPickerViewModel.VariationListItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colors.surface)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            items(state.variations) { variation ->
                VariationItem(
                    title = variation.title,
                    imageUrl = variation.imageUrl,
                    modifier = Modifier
                        .clickable { onVariationClick(variation) }
                        .padding(16.dp)
                )
                Divider(
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)),
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
            }
            if (state.loadingState == VariationPickerViewModel.LoadingState.APPENDING) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                    )
                }
            }
        }

        InfiniteListHandler(listState = listState, buffer = 3) {
            onLoadMore()
        }
    }
}

@Composable
fun VariationItem(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_product),
            error = painterResource(R.drawable.ic_product),
            contentDescription = stringResource(R.string.product_image_content_description),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .size(dimensionResource(R.dimen.major_300))
                .clip(RoundedCornerShape(3.dp))
        )

        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview
@Composable
fun VariationItemPreview() {
    WooThemeWithBackground {
        VariationItem(
            title = "This the product title",
            imageUrl = "not valid url",
        )
    }
}
