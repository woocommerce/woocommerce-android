package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreateCouponEditViewModel @Inject constructor(
    private val validator: CouponValidator,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: OrderCreateCouponEditFragmentArgs by savedState.navArgs()
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
            val initialCouponCode = navArgs.couponCode
            if (initialCouponCode.isNullOrEmpty()) {
                triggerEvent(CouponEditResult.AddNewCouponCode(couponCode))
            } else {
                triggerEvent(CouponEditResult.UpdateCouponCode(initialCouponCode, couponCode))
            }
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
            triggerEvent(CouponEditResult.RemoveCoupon(it))
        }
    }

    data class ViewState(
        val isDoneButtonEnabled: Boolean = false,
        val couponCode: String = "",
        val isRemoveButtonVisible: Boolean = false,
        val validationState: ValidationState = ValidationState.IDLE,
    )

    enum class ValidationState { IDLE, IN_PROGRESS, ERROR }

    @Parcelize
    sealed class CouponEditResult(
        open val orderDraft: Order? = null
    ) : MultiLiveEvent.Event(), Parcelable {
        @Parcelize
        data class UpdateCouponCode(
            val oldCode: String,
            val newCode: String,
            override val orderDraft: Order? = null
        ) : CouponEditResult()

        @Parcelize
        data class AddNewCouponCode(val couponCode: String, override val orderDraft: Order? = null) : CouponEditResult()

        @Parcelize
        data class RemoveCoupon(val couponCode: String, override val orderDraft: Order? = null) : CouponEditResult()
    }
}
