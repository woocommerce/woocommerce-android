package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CouponDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponDetailsRepository: CouponDetailsRepository,
    private val couponUtils: CouponUtils
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<CouponDetailsFragmentArgs>()
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val couponDetails = couponDetailsRepository.loadCoupon(navArgs.couponId)
        .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    private val couponPerformance = loadCouponPerformance()

    val couponState = combine(couponDetails, couponPerformance) { details, couponPerformance ->
        CouponDetailsState(coupon = details, couponPerformanceState = couponPerformance)
    }.onStart {
        emit(CouponDetailsState(isLoading = true))
    }.catch {
        // TODO trigger an error Snackbar and navigate up
    }.asLiveData()

    private fun loadCouponPerformance() = flow {
        emit(CouponPerformanceState.Loading())
        couponDetailsRepository.fetchCouponPerformance(navArgs.couponId)
            .fold(
                onSuccess = {
                    val performanceUi = CouponPerformanceUi(
                        formattedAmount = couponUtils.formatCurrency(it.amount, currencyCode),
                        ordersCount = it.ordersCount
                    )
                    emit(CouponPerformanceState.Success(performanceUi))
                },
                onFailure = { emit(CouponPerformanceState.Failure()) }
            )
    }.combine(couponDetails) { couponPerformanceState, couponDetails ->
        when {
            couponPerformanceState !is CouponPerformanceState.Success && couponDetails?.usageCount == 0 -> {
                // Shortcut for displaying 0 without loading
                CouponPerformanceState.Success(
                    data = CouponPerformanceUi(
                        formattedAmount = couponUtils.formatCurrency(BigDecimal.ZERO, currencyCode),
                        ordersCount = 0
                    )
                )
            }
            couponPerformanceState is CouponPerformanceState.Loading -> {
                CouponPerformanceState.Loading(couponDetails?.usageCount)
            }
            couponPerformanceState is CouponPerformanceState.Failure -> {
                CouponPerformanceState.Failure(couponDetails?.usageCount)
            }
            else -> couponPerformanceState
        }
    }

    data class CouponDetailsState(
        val isLoading: Boolean = false,
        val coupon: CouponUi? = null,
        val couponPerformanceState: CouponPerformanceState? = null
    )

    data class CouponUi(
        val id: Long,
        val code: String? = null,
        val amount: BigDecimal? = null,
        val usageCount: Int? = null,
        val formattedDiscount: String,
        val affectedArticles: String,
        val formattedSpendingInfo: String,
        val isActive: Boolean
    )

    data class CouponPerformanceUi(
        val ordersCount: Int,
        val formattedAmount: String
    )

    sealed class CouponPerformanceState {
        abstract val ordersCount: Int?

        data class Loading(override val ordersCount: Int? = null) : CouponPerformanceState()
        data class Failure(override val ordersCount: Int? = null) : CouponPerformanceState()
        data class Success(val data: CouponPerformanceUi) : CouponPerformanceState() {
            override val ordersCount: Int
                get() = data.ordersCount
        }
    }
}
