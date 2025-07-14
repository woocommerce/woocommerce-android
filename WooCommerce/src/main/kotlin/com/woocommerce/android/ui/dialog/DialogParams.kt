package com.woocommerce.android.ui.dialog

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class DialogParams(
    @StringRes val titleId: Int? = null,
    @StringRes val messageId: Int? = null,
    @StringRes val positiveButtonId: Int? = null,
    @StringRes val negativeButtonId: Int? = null,
    @StringRes val neutralButtonId: Int? = null,
    val cancelable: Boolean = true
) : Parcelable
