package com.woocommerce.android.ui.blaze.creation.ad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
                title = stringResource(id = R.string.blaze_campaign_product_picker_title),
                onNavigationButtonClick = onBackButtonTapped,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            columns = GridCells.Adaptive(minSize = 128.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(viewState.productImages) { photoUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl.source)
                        .crossfade(true)
                        .build(),
                    fallback = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    placeholder = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    error = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clickable { onImageSelected(photoUrl) }
                )
            }
        }
    }
}
