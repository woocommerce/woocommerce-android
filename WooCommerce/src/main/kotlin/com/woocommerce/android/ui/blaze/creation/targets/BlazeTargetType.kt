package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class BlazeTargetType : Parcelable {
    LANGUAGE,
    DEVICE,
    INTEREST,
    LOCATION
}
