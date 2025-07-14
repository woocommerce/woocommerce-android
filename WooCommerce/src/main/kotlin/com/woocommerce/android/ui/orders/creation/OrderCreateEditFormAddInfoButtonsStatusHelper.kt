package com.woocommerce.android.ui.orders.creation

import androidx.core.view.isVisible
import com.woocommerce.android.databinding.FragmentOrderCreateEditFormBinding
import javax.inject.Inject

class OrderCreateEditFormAddInfoButtonsStatusHelper @Inject constructor() {
    fun changeAddInfoButtonsEnabledState(
        binding: FragmentOrderCreateEditFormBinding,
        addInfoButtonsStateTransition: AddInfoButtonsStateTransition,
    ) {
        binding.additionalInfoCollectionSection.run {
            root.post {
                when (val state = addInfoButtonsStateTransition.isAddShippingButtonState) {
                    is AddInfoButtonsStateTransition.State.Change -> {
                        addShippingButton.isEnabled = state.enabled
                        if (addShippingButtonGroup.isVisible) {
                            addShippingButtonLockIcon.isVisible = !state.enabled
                        }
                    }

                    AddInfoButtonsStateTransition.State.Keep -> {}
                }

                when (val state = addInfoButtonsStateTransition.isAddCouponButtonState) {
                    is AddInfoButtonsStateTransition.State.Change -> {
                        addCouponButton.isEnabled = state.enabled
                        if (addCouponButtonGroup.isVisible) {
                            addCouponButtonLockIcon.isVisible = !state.enabled
                        }
                    }

                    AddInfoButtonsStateTransition.State.Keep -> {}
                }

                when (val state = addInfoButtonsStateTransition.isAddGiftCardButtonState) {
                    is AddInfoButtonsStateTransition.State.Change -> {
                        addGiftCardButton.isEnabled = state.enabled
                        if (addGiftCardButtonGroup.isVisible) {
                            addGiftCardButtonLockIcon.isVisible = !state.enabled
                        }
                    }

                    AddInfoButtonsStateTransition.State.Keep -> {}
                }
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
