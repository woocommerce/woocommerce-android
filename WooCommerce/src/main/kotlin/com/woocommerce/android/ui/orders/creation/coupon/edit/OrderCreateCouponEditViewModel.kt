package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreateCouponEditViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: OrderCreateCouponEditFragmentArgs by savedState.navArgs()
    private val couponCode = savedState.getNullableStateFlow(
        viewModelScope,
        navArgs.couponCode,
        String::class.java,
        "key_coupon_code"
    )

    val viewState = couponCode.map {
        ViewState(couponCode = it ?: "")
    }.asLiveData()

    fun onCouponRemoved() {
        navArgs.couponCode?.let {
            triggerEvent(CouponEditResult.RemoveCoupon(it))
        }
    }

    data class ViewState(
        val couponCode: String = "",
        val isRemoveButtonVisible: Boolean = couponCode.isNotNullOrEmpty(),
    )

    @Parcelize
    sealed class CouponEditResult : MultiLiveEvent.Event(), Parcelable {

        @Parcelize
        data class RemoveCoupon(val couponCode: String) : CouponEditResult()
    }
}
