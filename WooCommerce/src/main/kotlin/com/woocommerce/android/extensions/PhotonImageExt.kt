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
 */
@Composable
fun rememberPhotonImageRequest(
    originalUrl: String,
    imageSizePx: Int,
    configure: ImageRequest.Builder.() -> Unit = {}
): ImageRequest {
    val context = LocalContext.current
    val photonUrl = remember(originalUrl, imageSizePx) {
        if (originalUrl.isNotEmpty()) {
            PhotonUtils.getPhotonImageUrl(originalUrl, imageSizePx, imageSizePx)
        } else {
            originalUrl
        }
    }

    var currentUrl by remember(originalUrl, photonUrl) { mutableStateOf(photonUrl) }

    return remember(currentUrl) {
        ImageRequest.Builder(context)
            .data(currentUrl)
            .apply(configure)
            .listener(
                onError = { _, _ ->
                    if (currentUrl != originalUrl && originalUrl.isNotEmpty()) {
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
    quality: PhotonUtils.Quality = PhotonUtils.Quality.HIGH
): RequestBuilder<Drawable> {
    if (originalUrl.isNullOrEmpty()) {
        return this.load(originalUrl)
    }
    val photonUrl = PhotonUtils.getPhotonImageUrl(originalUrl, width, height, quality)
    return this.load(photonUrl).error(this.load(originalUrl))
}

fun RequestManager.loadPhotonUrlWithFallback(
    originalUrl: String?,
    width: Int,
    height: Int
): RequestBuilder<Drawable> {
    if (originalUrl.isNullOrEmpty()) {
        return this.load(originalUrl)
    }
    val photonUrl = PhotonUtils.getPhotonImageUrl(originalUrl, width, height)
    return this.load(photonUrl).error(this.load(originalUrl))
}

fun <T> RequestBuilder<T>.loadPhotonUrlWithFallback(
    originalUrl: String?,
    width: Int,
    height: Int,
    quality: PhotonUtils.Quality = PhotonUtils.Quality.HIGH
): RequestBuilder<T> {
    if (originalUrl.isNullOrEmpty()) {
        return this.load(originalUrl)
    }
    val photonUrl = PhotonUtils.getPhotonImageUrl(originalUrl, width, height, quality)
    return this.load(photonUrl).error(this.clone().load(originalUrl))
}

fun <T> RequestBuilder<T>.loadPhotonUrlWithFallback(
    originalUrl: String?,
    width: Int,
    height: Int
): RequestBuilder<T> {
    if (originalUrl.isNullOrEmpty()) {
        return this.load(originalUrl)
    }
    val photonUrl = PhotonUtils.getPhotonImageUrl(originalUrl, width, height)
    return this.load(photonUrl).error(this.clone().load(originalUrl))
}
