package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
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

    val viewState = couponDraft
        .filterNotNull()
        .map { coupon ->
            ViewState(
                couponDraft = coupon,
                localizedType = coupon.type?.let { couponUtils.localizeType(it) },
                amountUnit = if (coupon.type == Coupon.Type.Percent) "%" else currencyCode,
                hasChanges = !coupon.isSameCoupon(storedCoupon.await())
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

    data class ViewState(
        val couponDraft: Coupon,
        val localizedType: String?,
        val amountUnit: String,
        val hasChanges: Boolean
    )
}
