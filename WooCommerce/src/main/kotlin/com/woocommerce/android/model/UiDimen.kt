package com.woocommerce.android.model

import androidx.annotation.DimenRes

/**
 * [UiDimen] is a utility sealed class that represents a dimension to be used in the UI. It allows a dimension to be
 * represented as both dimen resource and integer.
 */
sealed class UiDimen {
    data class UiDimenRes(@DimenRes val dimenRes: Int) : UiDimen()
    data class UiDimenDPInt(val dimensionDP: Int) : UiDimen()
}
