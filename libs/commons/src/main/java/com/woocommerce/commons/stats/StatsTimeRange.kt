package com.woocommerce.commons.stats

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class StatsTimeRange(
    val start: Date,
    val end: Date
) : Parcelable
