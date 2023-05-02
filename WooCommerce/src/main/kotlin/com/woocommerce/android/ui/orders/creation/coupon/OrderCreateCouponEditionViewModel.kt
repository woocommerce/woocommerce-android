package com.woocommerce.android.ui.orders.creation.coupon

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
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OrderCreateCouponEditionViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: OrderCreateCouponEditionFragmentArgs by savedState.navArgs()
    private val couponCode = savedState.getNullableStateFlow(
        viewModelScope,
        navArgs.couponCode,
        String::class.java,
        "coupon_code"
    )

    val viewState = couponCode.map { code ->
        ViewState(
            isDoneButtonEnabled = code?.isNotNullOrEmpty() ?: false,
            couponCode = code ?: "",
            isRemoveButtonVisible = navArgs.couponCode.isNotNullOrEmpty()
        )
    }.asLiveData()

    fun onDoneClicked() {
        triggerEvent(UpdateCouponCode(couponCode.value))
    }

    fun onCouponCodeChanged(newCode: String) {
        couponCode.update {
            newCode
        }
    }

    fun onCouponRemoved() {
        couponCode.update {
            ""
        }
        triggerEvent(UpdateCouponCode(couponCode.value))
    }

    data class ViewState(
        val isDoneButtonEnabled: Boolean = false,
        val couponCode: String = "",
        val isRemoveButtonVisible: Boolean = false,
    )

    data class UpdateCouponCode(val couponCode: String?) : MultiLiveEvent.Event()
}
