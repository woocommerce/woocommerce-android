package com.woocommerce.android.ui.compose.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.woocommerce.android.R
import com.woocommerce.android.extensions.rememberPhotonImageRequest

@Composable
fun ProductThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageSize: Dp = 42.dp,
    @DrawableRes placeHolderDrawableId: Int = R.drawable.ic_product,
    @DrawableRes fallbackDrawableId: Int = R.drawable.ic_product,
    @DrawableRes errorDrawableId: Int = R.drawable.ic_product,
    contentDescription: String = "",
    cornerRadius: Dp = 4.dp,
) {
    val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx() }
    val imageRequest = rememberPhotonImageRequest(
        originalUrl = imageUrl,
        width = imageSizePx,
        height = imageSizePx,
    ) {
        crossfade(true)
        placeholder(placeHolderDrawableId)
        fallback(fallbackDrawableId)
        error(errorDrawableId)
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(imageSize)
            .clip(shape = RoundedCornerShape(cornerRadius))
    )
}
