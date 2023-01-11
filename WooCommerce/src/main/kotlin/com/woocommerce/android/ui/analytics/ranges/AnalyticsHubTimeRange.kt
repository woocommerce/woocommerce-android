package com.woocommerce.android.ui.analytics.ranges

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
) : Parcelable
