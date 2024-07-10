package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Image : Parcelable {
    val uri: String

    @Parcelize
    data class LocalImage(override val uri: String) : Image

    @Parcelize
    data class WPMediaLibraryImage(val content: Product.Image) : Image {
        override val uri: String
            get() = content.source
    }
}
