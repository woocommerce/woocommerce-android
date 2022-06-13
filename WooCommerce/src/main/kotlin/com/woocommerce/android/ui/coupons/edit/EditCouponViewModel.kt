package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.Coupon.CouponRestrictions
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditIncludedProductCategories
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditIncludedProducts
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenCouponRestrictions
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class EditCouponViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val couponRepository: CouponRepository,
    private val couponUtils: CouponUtils,
    private val parameterRepository: ParameterRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
    }

    private val navArgs: EditCouponFragmentArgs by savedStateHandle.navArgs()
    private val storedCoupon: Deferred<Coupon> = async {
        couponRepository.observeCoupon(navArgs.couponId).first()
    }

    private val couponDraft = savedStateHandle.getNullableStateFlow(viewModelScope, null, Coupon::class.java)
    private val currencyCode
        get() = parameterRepository.getParameters(PARAMETERS_KEY, savedState).currencySymbol.orEmpty()

    private val isSaving = MutableStateFlow(false)

    val viewState = combine(
        flow = couponDraft
            .filterNotNull(),
        flow2 = isSaving
    ) { coupon, isSaving ->
        ViewState(
            couponDraft = coupon,
            localizedType = coupon.type?.let { couponUtils.localizeType(it) },
            amountUnit = if (coupon.type == Coupon.Type.Percent) "%" else currencyCode,
            hasChanges = !coupon.isSameCoupon(storedCoupon.await()),
            isSaving = isSaving
        )
    }
        .asLiveData()

    init {
        if (couponDraft.value == null) {
            launch {
                couponDraft.value = storedCoupon.await()
            }
        }
    }

    fun onAmountChanged(value: BigDecimal?) {
        couponDraft.update {
            it?.copy(amount = value)
        }
    }

    fun onCouponCodeChanged(code: String) {
        couponDraft.update {
            it?.copy(code = code)
        }
    }

    fun onRegenerateCodeClick() {
        val newCode = couponUtils.generateRandomCode()
        couponDraft.update {
            it?.copy(code = newCode)
        }
    }

    fun onDescriptionButtonClick() {
        triggerEvent(OpenDescriptionEditor(couponDraft.value?.description))
    }

    fun onDescriptionChanged(description: String) {
        couponDraft.update {
            it?.copy(description = description)
        }
    }

    fun onExpiryDateChanged(expiryDate: Date?) {
        couponDraft.update {
            it?.copy(dateExpires = expiryDate)
        }
    }

    fun onFreeShippingChanged(value: Boolean) {
        couponDraft.update {
            it?.copy(isShippingFree = value)
        }
    }

    fun onUsageRestrictionsClick() {
        couponDraft.value?.let {
            // If a Coupon has no set minimum or maximum spend restriction, the REST API returns their values
            // as BigDecimal `0.00`. Yet, in wp-admin, a Coupon with no minimum or maximum spend restriction has the
            // "No minimum" or "No maximum" placeholders displayed, instead of `0.00`.
            // To replicate that behavior on the app's Coupon Restriction Screen, here we set `0.00` values as null.
            val minimumAmount =
                if (it.restrictions.minimumAmount isEqualTo BigDecimal.ZERO) null else it.restrictions.minimumAmount
            val maximumAmount =
                if (it.restrictions.maximumAmount isEqualTo BigDecimal.ZERO) null else it.restrictions.maximumAmount
            triggerEvent(
                OpenCouponRestrictions(
                    restrictions = it.restrictions.copy(
                        minimumAmount = minimumAmount,
                        maximumAmount = maximumAmount
                    ),
                    currencyCode = currencyCode,
                    showLimitUsageToXItems = it.type != Coupon.Type.FixedCart
                )
            )
        }
    }

    fun onSelectProductsButtonClick() {
        couponDraft.value?.let {
            triggerEvent(EditIncludedProducts(it.productIds))
        }
    }

    fun onSelectedProductsUpdated(productIds: Set<Long>) {
        couponDraft.update {
            it?.copy(productIds = productIds.toList())
        }
    }

    fun onSelectCategoriesButtonClick() {
        couponDraft.value?.let {
            triggerEvent(EditIncludedProductCategories(it.categoryIds))
        }
    }

    fun onIncludedCategoriesChanged(includedCategoryIds: Set<Long>) {
        couponDraft.update {
            it?.copy(categoryIds = includedCategoryIds.toList())
        }
    }

    fun onRestrictionsUpdated(restrictions: CouponRestrictions) {
        couponDraft.update {
            it?.copy(restrictions = restrictions)
        }
    }

    fun onSaveClick() = launch {
        isSaving.value = true
        couponRepository.updateCoupon(couponDraft.value!!)
            .fold(
                onSuccess = {
                    triggerEvent(ShowSnackbar(R.string.coupon_edit_coupon_updated))
                    triggerEvent(Exit)
                },
                onFailure = { exception ->
                    val message = (exception as? WooException)?.takeIf { it.error.type == WooErrorType.GENERIC_ERROR }
                        ?.message?.let { UiString.UiStringText(it) }
                        ?: UiString.UiStringRes(R.string.coupon_edit_coupon_update_failed)
                    triggerEvent(ShowUiStringSnackbar(message))
                }
            )
        isSaving.value = false
    }

    data class ViewState(
        val couponDraft: Coupon,
        val localizedType: String?,
        val amountUnit: String,
        val hasChanges: Boolean,
        val isSaving: Boolean
    )
}
