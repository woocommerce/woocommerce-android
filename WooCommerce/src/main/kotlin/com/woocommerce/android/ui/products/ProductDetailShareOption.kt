package com.woocommerce.android.ui.products

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.woocommerce.android.R

@Composable
fun ProductDetailShareOption(viewModel: ProductDetailViewModel) {
    val productState = viewModel.productDetailViewStateData.liveData.observeAsState()
    ProductDetailShareOption(
        productState,
        viewModel::onShareProductPageClicked
    )
}

@Composable
fun ProductDetailShareOption(
    state: State<ProductDetailViewModel.ProductDetailViewState?>,
    onShareProductPageClick: () -> Unit
) {
    Column {
        state.value?.productDraft?.let { product ->
            Text(
                text = "Share Product",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
            )

            if (product.images.isNotEmpty()) {
                Text(
                    text = "Share Image:",
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(product.images) { image ->
                        ProductDetailShareImage(imageUrl = image.source)
                    }
                }
            }

            OutlinedButton(
                onClick = { onShareProductPageClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "Share Product Page",
                    color = colorResource(id = R.color.color_primary)
                )
            }
        }
    }
}

@Composable
private fun ProductDetailShareImage(imageUrl: String) {
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    if (imageUrl.isNotEmpty()) {
        Glide.with(LocalContext.current)
            .asBitmap()
            .load(imageUrl)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        bitmapState.value = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Nothing to do here.
                    }
                }
            )
    }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(125.dp),
        elevation = 1.dp
    ) {
        bitmapState.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(id = R.string.more_menu_avatar),
                contentScale = ContentScale.Crop,
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.img_gravatar_placeholder),
            contentDescription = stringResource(id = R.string.more_menu_avatar),
            contentScale = ContentScale.Crop
        )
    }
}
