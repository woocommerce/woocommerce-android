package com.woocommerce.android.ui.prefs

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class RequestedAnalyticsValue : Parcelable {
    NONE, ENABLED, DISABLE
}
