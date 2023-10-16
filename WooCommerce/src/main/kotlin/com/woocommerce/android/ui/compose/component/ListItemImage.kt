package com.woocommerce.android.ui.compose.component

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

@Composable
fun ListItemImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    @DrawableRes placeHolderDrawableId: Int,
    contentDescription: String = ""
) {
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
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
    bitmapState.value?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription,
            modifier = modifier
        )
    } ?: Image(
        painter = painterResource(id = placeHolderDrawableId),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
