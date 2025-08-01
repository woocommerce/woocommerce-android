package com.woocommerce.android.ui.products.images.ai

import android.graphics.Bitmap
import android.net.Uri

sealed class ViewState {
    abstract val showBackDialog: Boolean

    data class BackgroundProcessingInProgress(
        val imageUri: Uri,
        override val showBackDialog: Boolean = false
    ) : ViewState()

    data class Success(
        val bitmap: Bitmap,
        override val showBackDialog: Boolean = false
    ) : ViewState()
}
