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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OrderCreateCouponEditionViewModel @Inject constructor(
    private val validator: CouponValidator,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: OrderCreateCouponEditionFragmentArgs by savedState.navArgs()
    private val couponCode = savedState.getNullableStateFlow(
        viewModelScope,
        navArgs.couponCode,
        String::class.java,
        "key_coupon_code"
    )

    private val validationState = savedState.getNullableStateFlow(
        viewModelScope,
        ValidationState.IDLE,
        ValidationState::class.java,
        "key_validation_state"
    )

    val viewState = combine(couponCode, validationState) { code, validation ->
        val isDoneButtonEnabled = code?.isNotNullOrEmpty() == true && validation == ValidationState.IDLE
        ViewState(
            isDoneButtonEnabled = isDoneButtonEnabled,
            couponCode = code ?: "",
            isRemoveButtonVisible = navArgs.couponCode.isNotNullOrEmpty(),
            validationState = validation
        )
    }.asLiveData()

    suspend fun onDoneClicked() {
        validationState.update {
            ValidationState.IN_PROGRESS
        }
        val couponCode = couponCode.value
        if (couponCode != null && validator.isCouponValid(couponCode)) {
            validationState.update {
                ValidationState.IDLE
            }
            triggerEvent(UpdateCouponCode(couponCode))
        } else {
            validationState.update {
                ValidationState.ERROR
            }
        }
    }

    fun onCouponCodeChanged(newCode: String) {
        validationState.update {
            ValidationState.IDLE
        }
        couponCode.update {
            newCode
        }
    }

    fun onCouponRemoved() {
        navArgs.couponCode?.let {
            triggerEvent(RemoveCoupon(it))
        }
    }

    data class ViewState(
        val isDoneButtonEnabled: Boolean = false,
        val couponCode: String = "",
        val isRemoveButtonVisible: Boolean = false,
        val validationState: ValidationState = ValidationState.IDLE,
    )

    enum class ValidationState { IDLE, IN_PROGRESS, ERROR }

    data class UpdateCouponCode(val couponCode: String) : MultiLiveEvent.Event()
    data class RemoveCoupon(val couponCode: String) : MultiLiveEvent.Event()
}
