package com.woocommerce.android.ui.searchfilter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class SearchFilterItem(
    val name: String,
    val value: String
) : Parcelable
