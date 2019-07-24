package com.woocommerce.android.viewmodel.view

import android.os.Parcelable
import com.woocommerce.android.viewmodel.view.IMvvmViewState
import kotlinx.android.parcel.Parcelize

@Parcelize
class MvvmCustomViewStateWrapper(
    val superState: Parcelable?,
    val state: IMvvmViewState?
): Parcelable
