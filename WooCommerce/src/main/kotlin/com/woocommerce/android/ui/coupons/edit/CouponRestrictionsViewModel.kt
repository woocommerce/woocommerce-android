package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Coupon.CouponRestrictions
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class CouponRestrictionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: CouponRestrictionsFragmentArgs by savedState.navArgs()

    private val restrictionsDraft = savedStateHandle.getStateFlow(
        viewModelScope,
        // Casting below added to work around IDE bug of not recognizing the nested class type.
        navArgs.restrictions as CouponRestrictions
    )

    val viewState = restrictionsDraft
        .map { restrictions ->
            ViewState(
                restrictions = restrictions,
                currencyCode = navArgs.currencyCode,
                hasChanges = !restrictions.isSameRestrictions(navArgs.restrictions)
            )
        }.asLiveData()

    fun onBackPressed() {
        val event = viewState.value?.takeIf { it.hasChanges }?.let { viewState ->
            ExitWithResult(viewState.restrictions)
        } ?: Exit

        triggerEvent(event)
    }

    data class ViewState(
        val restrictions: CouponRestrictions,
        val currencyCode: String,
        val hasChanges: Boolean
    )
}
