package com.woocommerce.android.extensions

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import org.wordpress.android.util.PhotonUtils

/**
 * Coil/Compose extension for loading a Photon image with a fallback to the original URL.
 * Generates an ImageRequest that tries the Photon URL first, and if it fails, falls back to the original URL.
 * If the URL is already a Photon URL, no fallback is applied since the "original" would be the same URL.
 */
@Composable
fun rememberPhotonImageRequest(
    originalUrl: String,
    imageSizePx: Int,
    configure: ImageRequest.Builder.() -> Unit = {}
): ImageRequest {
    val context = LocalContext.current
    val photonUrl = remember(originalUrl, imageSizePx) {
        PhotonUtils.getPhotonImageUrl(originalUrl, imageSizePx, imageSizePx)
    }

    var currentUrl by remember(originalUrl, photonUrl) { mutableStateOf(photonUrl) }

    return remember(currentUrl, context) {
        ImageRequest.Builder(context)
            .data(currentUrl)
            .apply(configure)
            .listener(
                onError = { _, _ ->
                    if (!originalUrl.isPhotonUrl() && currentUrl != originalUrl) {
                        currentUrl = originalUrl
                    }
                }
            )
            .build()
    }
}

fun RequestManager.loadPhotonUrlWithFallback(
    originalUrl: String?,
    width: Int,
    height: Int,
    quality: PhotonUtils.Quality = PhotonUtils.Quality.MEDIUM
): RequestBuilder<Drawable> {
    if (originalUrl.isNullOrEmpty()) {
        return this.load(originalUrl)
    }
    val photonUrl = PhotonUtils.getPhotonImageUrl(originalUrl, width, height, quality)
    if (originalUrl.isPhotonUrl()) {
        return this.load(photonUrl)
    }
    return this.load(photonUrl).error(this.load(originalUrl))
}

fun <T> RequestBuilder<T>.loadPhotonUrlWithFallback(
    originalUrl: String?,
    width: Int,
    height: Int,
    quality: PhotonUtils.Quality = PhotonUtils.Quality.MEDIUM
): RequestBuilder<T> {
    if (originalUrl.isNullOrEmpty()) {
        return this.load(originalUrl)
    }
    val photonUrl = PhotonUtils.getPhotonImageUrl(originalUrl, width, height, quality)
    if (originalUrl.isPhotonUrl()) {
        return this.load(photonUrl)
    }
    return this.load(photonUrl).error(this.clone().load(originalUrl))
}

private fun String.isPhotonUrl(): Boolean {
    return contains("i0.wp.com") || contains("i1.wp.com") || contains("i2.wp.com")
}
