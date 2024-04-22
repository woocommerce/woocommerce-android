package com.woocommerce.android.ui.compose.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R

@Composable
fun ProductThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
    @DrawableRes placeHolderDrawableId: Int = R.drawable.ic_product,
    @DrawableRes fallbackDrawableId: Int = R.drawable.ic_product,
    @DrawableRes errorDrawableId: Int = R.drawable.ic_product,
    contentDescription: String = ""
) {
    AsyncImage(
        model = Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .placeholder(placeHolderDrawableId)
            .fallback(fallbackDrawableId)
            .error(errorDrawableId)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(42.dp)
            .clip(shape = RoundedCornerShape(4.dp))
    )
}
