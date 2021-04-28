package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * [UiString] is a utility sealed class that represents a string to be used in the UI. It allows a string to be
 * represented as both string resource and text.
 */
sealed class UiString : Parcelable {
    @Parcelize data class UiStringText(val text: String) : UiString()
    @Parcelize data class UiStringRes(@StringRes val stringRes: Int) : UiString()
}
