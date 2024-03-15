package com.woocommerce.android.ui.mystore.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class DateRange(
    val startDate: Date,
    val endDate: Date
) : Parcelable
