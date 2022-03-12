package com.woocommerce.android.ui.products

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
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
        viewModel::onShareProductPageClicked,
        viewModel::onShareProductImageClicked
    )
}

@Composable
fun ProductDetailShareOption(
    state: State<ProductDetailViewModel.ProductDetailViewState?>,
    onShareProductPageClick: () -> Unit,
    onShareProductImageClick: (image: Bitmap?) -> Unit
) {
    Column {
        state.value?.productDraft?.let { product ->
            Text(
                text = stringResource(id = R.string.product_share_dialog_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
            )

            if (product.images.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.product_share_dialog_image),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(product.images) { image ->
                        ProductDetailShareImage(
                            imageUrl = image.source,
                            onShareProductImageClick
                        )
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
                    text = stringResource(id = R.string.product_share_dialog_page_button),
                    color = colorResource(id = R.color.color_primary)
                )
            }
        }
    }
}

@Composable
private fun ProductDetailShareImage(
    imageUrl: String,
    onImageClick: (image: Bitmap?) -> Unit
) {
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
            .height(125.dp)
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = { onImageClick(bitmapState.value) }
            ),
        elevation = 1.dp
    ) {
        bitmapState.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(id = R.string.product_share_dialog_image_label),
                contentScale = ContentScale.Crop,
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.ic_gridicons_image),
            contentDescription = stringResource(id = R.string.product_share_dialog_image_label),
            contentScale = ContentScale.Crop
        )
    }
}
