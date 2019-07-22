package com.woocommerce.android.viewmodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class MvvmCustomViewStateWrapper(
    val superState: Parcelable?,
    val state: IMvvmViewState?
): Parcelable
