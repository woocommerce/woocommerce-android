package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
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

    private val couponSummary = loadCouponSummary()
    private val couponPerformance = loadCouponPerformance()

    val couponState = combine(couponSummary, couponPerformance) { couponSummary, couponPerformance ->
        CouponDetailsState(couponSummary = couponSummary, couponPerformanceState = couponPerformance)
    }.onStart {
        emit(CouponDetailsState(isLoading = true))
    }.asLiveData()

    init {
        viewModelScope.launch {
            couponDetailsRepository.fetchCoupon(navArgs.couponId).onFailure {
                if (coupon.value == null) {
                    triggerEvent(ShowSnackbar(R.string.coupon_summary_loading_failure))
                    triggerEvent(Exit)
                }
            }
        }
    }

    private fun loadCouponSummary(): Flow<CouponSummaryUi> {
        return coupon
            .filterNotNull()
            .map { coupon ->
                CouponSummaryUi(
                    code = coupon.code,
                    summary = couponUtils.generateSummary(coupon, currencyCode),
                    discountType = coupon.type?.let { couponUtils.localizeType(it) },
                    minimumSpending = couponUtils.formatMinimumSpendingInfo(coupon.minimumAmount, currencyCode),
                    maximumSpending = couponUtils.formatMaximumSpendingInfo(coupon.maximumAmount, currencyCode),
                    isActive = coupon.dateExpiresGmt?.after(Date()) ?: true,
                    expiration = coupon.dateExpiresGmt?.let { couponUtils.formatExpirationDate(it) },
                    shareCodeMessage = couponUtils.formatSharingMessage(
                        amount = coupon.amount,
                        currencyCode = currencyCode,
                        couponCode = coupon.code,
                        includedProducts = coupon.products.size,
                        excludedProducts = coupon.excludedProducts.size
                    )
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

    fun onCopyButtonClick() {
        couponState.value?.couponSummary?.code?.let {
            triggerEvent(CopyCodeEvent(it))
        }
    }

    fun onShareButtonClick() {
        couponState.value?.couponSummary?.shareCodeMessage?.let {
            triggerEvent(ShareCodeEvent(it))
        } ?: run {
            triggerEvent(ShowSnackbar(R.string.coupon_details_share_formatting_failure))
        }
    }

    data class CouponDetailsState(
        val isLoading: Boolean = false,
        val couponSummary: CouponSummaryUi? = null,
        val couponPerformanceState: CouponPerformanceState? = null
    )

    data class CouponSummaryUi(
        val code: String?,
        val isActive: Boolean,
        val summary: String,
        val discountType: String?,
        val minimumSpending: String?,
        val maximumSpending: String?,
        val expiration: String?,
        val shareCodeMessage: String?
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

    data class CopyCodeEvent(val couponCode: String) : MultiLiveEvent.Event()
    data class ShareCodeEvent(val shareCodeMessage: String) : MultiLiveEvent.Event()
}
