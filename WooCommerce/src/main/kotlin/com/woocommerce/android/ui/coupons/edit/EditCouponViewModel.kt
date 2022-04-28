package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditCouponViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val editCouponRepository: EditCouponRepository,
    private val couponUtils: CouponUtils
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: EditCouponFragmentArgs by savedStateHandle.navArgs()
    private val storedCoupon: Deferred<Coupon> = async {
        editCouponRepository.getCoupon(navArgs.couponId)
    }

    private val couponDraft = savedStateHandle.getNullableStateFlow(viewModelScope, null, Coupon::class.java)

    val viewState = couponDraft
        .filterNotNull()
        .map { coupon ->
            ViewState(
                couponDraft = coupon,
                localizedType = coupon.type?.let { couponUtils.localizeType(it) }
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

    data class ViewState(
        val couponDraft: Coupon,
        val localizedType: String?
    )
}
