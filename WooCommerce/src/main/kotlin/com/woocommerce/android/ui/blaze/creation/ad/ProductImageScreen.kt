package com.woocommerce.android.ui.blaze.creation.ad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun ProductImagePickerScreen(viewModel: ProductImagePickerViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        ProductImagePickerScreen(
            viewState = viewState,
            onImageSelected = viewModel::onImageSelected,
            onBackButtonTapped = viewModel::onBackButtonTapped
        )
    }
}

@Composable
fun ProductImagePickerScreen(
    viewState: ProductImagePickerViewModel.ViewState,
    onImageSelected: (Product.Image) -> Unit,
    onBackButtonTapped: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_product_photo_picker_title),
                onNavigationButtonClick = onBackButtonTapped,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        when {
            viewState.productImages.isEmpty() -> ProductPhotosEmpty()

            else -> ProductImageGrid(
                viewState = viewState,
                onImageSelected = onImageSelected,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ProductPhotosEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.blaze_campaign_product_photo_picker_empty),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProductImageGrid(
    viewState: ProductImagePickerViewModel.ViewState,
    onImageSelected: (Product.Image) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        columns = Adaptive(minSize = 128.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(viewState.productImages) { photoUrl ->
            AsyncImage(
                model = Builder(LocalContext.current)
                    .data(photoUrl.source)
                    .crossfade(true)
                    .build(),
                fallback = painterResource(R.drawable.blaze_campaign_product_placeholder),
                placeholder = painterResource(R.drawable.blaze_campaign_product_placeholder),
                error = painterResource(R.drawable.blaze_campaign_product_placeholder),
                contentDescription = stringResource(
                    id = R.string.blaze_campaign_product_photo_picker_photo_content_description
                ),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clickable { onImageSelected(photoUrl) }
            )
        }
    }
}
