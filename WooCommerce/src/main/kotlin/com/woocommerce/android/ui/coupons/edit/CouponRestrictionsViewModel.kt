package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Coupon.CouponRestrictions
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditExcludedProductCategories
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditExcludedProducts
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
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
                hasChanges = !restrictions.isSameRestrictions(navArgs.restrictions),
                showLimitUsageToXItems = navArgs.showLimitUsageToXItems
            )
        }.asLiveData()

    fun onBackPressed() {
        val event = viewState.value?.takeIf { it.hasChanges }?.let { viewState ->
            ExitWithResult(viewState.restrictions)
        } ?: Exit

        triggerEvent(event)
    }

    fun onMinimumAmountChanged(value: BigDecimal?) {
        restrictionsDraft.update {
            it.copy(minimumAmount = value)
        }
    }

    fun onMaximumAmountChanged(value: BigDecimal?) {
        restrictionsDraft.update {
            it.copy(maximumAmount = value)
        }
    }

    fun onUsageLimitPerCouponChanged(value: Int?) {
        restrictionsDraft.update {
            it.copy(usageLimit = value)
        }
    }

    fun onLimitUsageToXItemsChanged(value: Int?) {
        restrictionsDraft.update {
            it.copy(limitUsageToXItems = value)
        }
    }

    fun onUsageLimitPerUserChanged(value: Int?) {
        restrictionsDraft.update {
            it.copy(usageLimitPerUser = value)
        }
    }

    fun onIndividualUseChanged(isForIndividualUse: Boolean) {
        restrictionsDraft.update {
            it.copy(isForIndividualUse = isForIndividualUse)
        }
    }

    fun onExcludeSaleItemsChanged(areSaleItemsExcluded: Boolean) {
        restrictionsDraft.update {
            it.copy(areSaleItemsExcluded = areSaleItemsExcluded)
        }
    }

    fun onAllowedEmailsButtonClicked() {
        triggerEvent(OpenAllowedEmailsEditor(restrictionsDraft.value.restrictedEmails))
    }

    fun onAllowedEmailsUpdated(allowedEmails: List<String>) {
        restrictionsDraft.update {
            it.copy(restrictedEmails = allowedEmails)
        }
    }

    fun onExcludeProductsButtonClick() {
        triggerEvent(EditExcludedProducts(restrictionsDraft.value.excludedProductIds))
    }

    fun onExcludeCategoriesButtonClick() {
        triggerEvent(EditExcludedProductCategories(restrictionsDraft.value.excludedCategoryIds))
    }

    fun onExcludedProductChanged(excludedProductIds: Set<Long>) {
        restrictionsDraft.update {
            it.copy(excludedProductIds = excludedProductIds.toList())
        }
    }

    fun onExcludedProductCategoriesChanged(excludedCategoryIds: Set<Long>) {
        restrictionsDraft.update {
            it.copy(excludedCategoryIds = excludedCategoryIds.toList())
        }
    }

    data class ViewState(
        val restrictions: CouponRestrictions,
        val currencyCode: String,
        val hasChanges: Boolean,
        val showLimitUsageToXItems: Boolean
    )

    data class OpenAllowedEmailsEditor(val allowedEmails: List<String>) : MultiLiveEvent.Event()
}
