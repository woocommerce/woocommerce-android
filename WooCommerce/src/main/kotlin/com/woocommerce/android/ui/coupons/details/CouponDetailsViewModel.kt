package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date
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

    private val coupon = couponDetailsRepository.observeCoupon(navArgs.couponId)
        .toStateFlow(null)

    private val couponUi = loadCouponDetails()
    private val couponPerformance = loadCouponPerformance()

    val couponState = combine(couponUi, couponPerformance) { couponUi, couponPerformance ->
        CouponDetailsState(coupon = couponUi, couponPerformanceState = couponPerformance)
    }.onStart {
        emit(CouponDetailsState(isLoading = true))
    }.asLiveData()

    init {
        viewModelScope.launch {
            couponDetailsRepository.fetchCoupon(navArgs.couponId).onFailure {
                triggerEvent(ShowSnackbar(R.string.coupon_details_performance_loading_failure))
                triggerEvent(Exit)
            }
        }
    }

    private fun loadCouponDetails(): Flow<CouponUi> {
        return coupon
            .filterNotNull()
            .map {
                CouponUi(
                    formattedDiscount = couponUtils.formatDiscount(it.amount, it.type, currencyCode),
                    formattedSpendingInfo = couponUtils.formatSpendingInfo(
                        it.minimumAmount,
                        it.maximumAmount,
                        currencyCode
                    ),
                    affectedArticles = couponUtils.formatAffectedArticles(
                        it.products.size,
                        it.excludedProducts.size,
                        it.categories.size,
                        it.excludedCategories.size
                    ),
                    isActive = it.dateExpiresGmt?.after(Date()) ?: true
                )
            }
    }

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
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.coupon_details_performance_loading_failure))
                    emit(CouponPerformanceState.Failure())
                }
            )
    }.combine(coupon) { couponPerformanceState, couponDetails ->
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
        val code: String? = null,
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
