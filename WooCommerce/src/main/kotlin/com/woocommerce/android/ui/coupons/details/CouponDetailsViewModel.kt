package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val couponRepository: CouponRepository,
    private val couponUtils: CouponUtils
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<CouponDetailsFragmentArgs>()
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val coupon = couponRepository.observeCoupon(navArgs.couponId)
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
            couponRepository.fetchCoupon(navArgs.couponId).onFailure {
                if (coupon.value == null) {
                    triggerEvent(ShowSnackbar(R.string.coupon_summary_loading_failure))
                    triggerEvent(Exit)
                }
            }
        }

        AnalyticsTracker.track(
            AnalyticsEvent.COUPON_DETAILS,
            mapOf(AnalyticsTracker.KEY_COUPON_ACTION to AnalyticsTracker.KEY_COUPON_ACTION_LOADED)
        )
    }

    private fun loadCouponSummary(): Flow<CouponSummaryUi> {
        return coupon
            .filterNotNull()
            .map { coupon ->
                CouponSummaryUi(
                    code = coupon.code,
                    isActive = coupon.dateExpires?.after(Date()) ?: true,
                    description = coupon.description,
                    summary = couponUtils.generateSummary(coupon, currencyCode),
                    isForIndividualUse = coupon.restrictions.isForIndividualUse ?: false,
                    isShippingFree = coupon.isShippingFree ?: false,
                    areSaleItemsExcluded = coupon.restrictions.areSaleItemsExcluded ?: false,
                    discountType = coupon.type?.let { couponUtils.localizeType(it) },
                    minimumSpending = couponUtils.formatMinimumSpendingInfo(
                        coupon.restrictions.minimumAmount,
                        currencyCode
                    ),
                    maximumSpending = couponUtils.formatMaximumSpendingInfo(
                        coupon.restrictions.maximumAmount,
                        currencyCode
                    ),
                    usageLimitPerUser = couponUtils.formatUsageLimitPerUser(coupon.restrictions.usageLimitPerUser),
                    usageLimitPerCoupon = couponUtils.formatUsageLimitPerCoupon(coupon.restrictions.usageLimit),
                    usageLimitPerItems = couponUtils.formatUsageLimitPerItems(coupon.restrictions.limitUsageToXItems),
                    expiration = coupon.dateExpires?.let { couponUtils.formatExpirationDate(it) },
                    emailRestrictions = couponUtils.formatRestrictedEmails(coupon.restrictions.restrictedEmails)
                )
            }
    }

    private fun loadCouponPerformance() = flow {
        emit(CouponPerformanceState.Loading())
        couponRepository.fetchCouponPerformance(navArgs.couponId)
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

    fun onDeleteButtonClick() {
        viewModelScope.launch {
            couponRepository.deleteCoupon(navArgs.couponId)
                .onFailure {
                    WooLog.e(
                        tag = WooLog.T.COUPONS,
                        message = "Coupon deletion failed: ${(it as WooException).error.message}"
                    )
                    triggerEvent(ShowSnackbar(R.string.coupon_details_delete_failure))
                }
                .onSuccess {
                    triggerEvent(ShowSnackbar(R.string.coupon_details_delete_successful))
                    triggerEvent(Exit)
                }
        }

        AnalyticsTracker.track(
            AnalyticsEvent.COUPON_DETAILS,
            mapOf(AnalyticsTracker.KEY_COUPON_ACTION to AnalyticsTracker.KEY_COUPON_ACTION_DELETED)
        )
    }

    fun onCopyButtonClick() {
        couponState.value?.couponSummary?.code?.let {
            triggerEvent(CopyCodeEvent(it))
        }

        AnalyticsTracker.track(
            AnalyticsEvent.COUPON_DETAILS,
            mapOf(AnalyticsTracker.KEY_COUPON_ACTION to AnalyticsTracker.KEY_COUPON_ACTION_COPIED)
        )
    }

    fun onShareButtonClick() {
        coupon.value?.let { coupon ->
            couponUtils.formatSharingMessage(
                amount = coupon.amount,
                currencyCode = currencyCode,
                couponCode = coupon.code,
                includedProducts = coupon.productIds.size,
                excludedProducts = coupon.restrictions.excludedProductIds.size
            )
        }?.let {
            triggerEvent(ShareCodeEvent(it))
        } ?: run {
            triggerEvent(ShowSnackbar(R.string.coupon_details_share_formatting_failure))
        }

        AnalyticsTracker.track(
            AnalyticsEvent.COUPON_DETAILS,
            mapOf(AnalyticsTracker.KEY_COUPON_ACTION to AnalyticsTracker.KEY_COUPON_ACTION_SHARED)
        )
    }

    fun onEditButtonClick() {
        coupon.value?.id?.let {
            triggerEvent(ShowEditCoupon(it))
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
        val description: String?,
        val summary: String,
        val isForIndividualUse: Boolean,
        val isShippingFree: Boolean,
        val areSaleItemsExcluded: Boolean,
        val discountType: String?,
        val minimumSpending: String?,
        val maximumSpending: String?,
        val usageLimitPerUser: String?,
        val usageLimitPerCoupon: String?,
        val usageLimitPerItems: String?,
        val expiration: String?,
        val emailRestrictions: String?
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
    data class ShowEditCoupon(val couponId: Long) : MultiLiveEvent.Event()
}
