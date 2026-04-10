package com.woocommerce.android.extensions

import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import org.wordpress.android.util.PhotonUtils

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
