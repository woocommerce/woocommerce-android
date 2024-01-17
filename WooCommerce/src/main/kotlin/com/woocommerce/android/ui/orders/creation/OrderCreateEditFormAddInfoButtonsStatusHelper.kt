package com.woocommerce.android.ui.orders.creation

import androidx.core.view.isVisible
import com.woocommerce.android.databinding.FragmentOrderCreateEditFormBinding
import javax.inject.Inject

class OrderCreateEditFormAddInfoButtonsStatusHelper @Inject constructor() {
    fun changeAddInfoButtonsEnabledState(
        binding: FragmentOrderCreateEditFormBinding,
        addInfoButtonsStateTransition: AddInfoButtonsStateTransition,
    ) {
        binding.additionalInfoCollectionSection.apply {
            when (addInfoButtonsStateTransition.isAddShippingButtonState) {
                is AddInfoButtonsStateTransition.State.Change -> {
                    addShippingButton.isEnabled = addInfoButtonsStateTransition.isAddShippingButtonState.enabled
                    addShippingButtonLockIcon.isVisible =
                        !addInfoButtonsStateTransition.isAddShippingButtonState.enabled
                }

                AddInfoButtonsStateTransition.State.Keep -> {}
            }

            when (addInfoButtonsStateTransition.isAddCouponButtonState) {
                is AddInfoButtonsStateTransition.State.Change -> {
                    addCouponButton.isEnabled = addInfoButtonsStateTransition.isAddCouponButtonState.enabled
                    addCouponButtonLockIcon.isVisible = !addInfoButtonsStateTransition.isAddCouponButtonState.enabled
                }

                AddInfoButtonsStateTransition.State.Keep -> {}
            }

            when (addInfoButtonsStateTransition.isAddGiftCardButtonState) {
                is AddInfoButtonsStateTransition.State.Change -> {
                    addGiftCardButton.isEnabled = addInfoButtonsStateTransition.isAddGiftCardButtonState.enabled
                    addGiftCardButtonLockIcon.isVisible =
                        !addInfoButtonsStateTransition.isAddGiftCardButtonState.enabled
                }

                AddInfoButtonsStateTransition.State.Keep -> {}
            }
        }
    }
}

data class AddInfoButtonsStateTransition(
    val isAddShippingButtonState: State = State.Keep,
    val isAddCouponButtonState: State = State.Keep,
    val isAddGiftCardButtonState: State = State.Keep
) {
    sealed class State {
        data class Change(val enabled: Boolean) : State()
        object Keep : State()
    }
}
