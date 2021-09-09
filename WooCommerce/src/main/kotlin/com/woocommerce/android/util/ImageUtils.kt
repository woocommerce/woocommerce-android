package com.woocommerce.android.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.ImageView
import com.woocommerce.android.di.GlideApp
import org.wordpress.android.util.AppLog

object ImageUtils {
    /**
     * Loads an image from the "imgUrl" into the ImageView and applies circle transformation.
     */
    fun loadUrlIntoCircle(
        context: Context,
        imageView: ImageView,
        imgUrl: String,
        placeholder: Drawable?
    ) {
        GlideApp.with(context)
            .load(imgUrl)
            .placeholder(placeholder)
            .circleCrop()
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads a base64 string without prefix (data:image/png;base64,) into the ImageView and applies circle
     * transformation.
     */
    fun loadBase64IntoCircle(
        context: Context,
        imageView: ImageView,
        base64ImageData: String,
        placeholder: Drawable?
    ) {
        val imageData: ByteArray
        try {
            val sanitizedBase64String = base64ImageData.replace("data:image/png;base64,", "")
            imageData = Base64.decode(sanitizedBase64String, Base64.DEFAULT)
        } catch (ex: IllegalArgumentException) {
            AppLog.e(AppLog.T.UTILS, String.format("Cant parse base64 image data:" + ex.message))
            return
        }

        GlideApp.with(context)
            .load(imageData)
            .placeholder(placeholder)
            .circleCrop()
            .into(imageView)
            .clearOnDetach()
    }
}
