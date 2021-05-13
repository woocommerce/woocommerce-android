package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * [UiString] is a utility sealed class that represents a string to be used in the UI. It allows a string to be
 * represented as both string resource and text.
 */
sealed class UiString(open val containsHtml: Boolean) : Parcelable {
    @Parcelize
    data class UiStringText(
        val text: String,
        override val containsHtml: Boolean = false
    ) : UiString(containsHtml)

    @Parcelize
    data class UiStringRes(
        @StringRes val stringRes: Int,
        val params: List<UiString> = emptyList(),
        override val containsHtml: Boolean = false
    ) : UiString(containsHtml)
}
