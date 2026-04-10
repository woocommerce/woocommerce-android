package com.woocommerce.android.ui.compose.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import org.wordpress.android.util.PhotonUtils

@Composable
fun ProductThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageSize: Dp = 42.dp,
    @DrawableRes placeHolderDrawableId: Int = R.drawable.ic_product,
    @DrawableRes fallbackDrawableId: Int = R.drawable.ic_product,
    @DrawableRes errorDrawableId: Int = R.drawable.ic_product,
    contentDescription: String = ""
) {
    val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx() }
    val photonUrl = remember(imageUrl, imageSizePx) {
        if (imageUrl.isNotEmpty()) {
            PhotonUtils.getPhotonImageUrl(imageUrl, imageSizePx, imageSizePx)
        } else {
            imageUrl
        }
    }

    var currentUrl by remember(imageUrl, photonUrl) { mutableStateOf(photonUrl) }

    AsyncImage(
        model = Builder(LocalContext.current)
            .data(currentUrl)
            .crossfade(true)
            .placeholder(placeHolderDrawableId)
            .fallback(fallbackDrawableId)
            .error(errorDrawableId)
            .listener(
                onError = { _, _ ->
                    if (currentUrl != imageUrl && imageUrl.isNotEmpty()) {
                        currentUrl = imageUrl
                    }
                }
            )
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(imageSize)
            .clip(shape = RoundedCornerShape(4.dp))
    )
}
