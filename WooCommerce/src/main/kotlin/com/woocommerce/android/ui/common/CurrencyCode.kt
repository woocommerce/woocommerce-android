package com.woocommerce.android.ui.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class CurrencyCode(val value: String) : Parcelable
