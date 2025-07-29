package com.woocommerce.android.ui.products.images.ai

import android.graphics.Bitmap
import android.net.Uri

sealed class ViewState {
    data class BackgroundProcessingInProgress(val imageUri: Uri) : ViewState()
    data class Success(val bitmap: Bitmap) : ViewState()
}
