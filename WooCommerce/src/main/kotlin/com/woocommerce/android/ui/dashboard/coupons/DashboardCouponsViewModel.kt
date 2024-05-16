package com.woocommerce.android.ui.dashboard.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.WooException
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardCouponsViewModel.Factory::class)
class DashboardCouponsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val couponRepository: CouponRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val COUPONS_LIMIT = 3
    }

    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)

    private val couponsReportCache: Pair<StatsTimeRange, List<CouponPerformanceReport>>? = null

    val viewState = refreshTrigger
        .onStart { emit(RefreshEvent()) }
        .transformLatest {
            emit(State.Loading)
            emitAll(
                observeCouponUiModels(it.isForced).map { result ->
                    result.fold(
                        onSuccess = { coupons -> State.Loaded(coupons) },
                        onFailure = { error ->
                            when {
                                error is WooException && error.error.type == WooErrorType.API_NOT_FOUND ->
                                    State.Error.WCAdminInactive

                                else -> State.Error.Generic
                            }
                        }
                    )
                }
            )
        }
        .asLiveData()

    private fun observeCouponUiModels(forceRefresh: Boolean): Flow<Result<List<CouponUiModel>>> =
        observeMostActiveCoupons(
            dateRange = SelectionType.MONTH_TO_DATE.generateSelectionData( // TODO pass date range
                referenceStartDate = Date(),
                referenceEndDate = Date(),
                calendar = Calendar.getInstance(),
                locale = Locale.getDefault()
            ).currentRange,
            forceRefresh = forceRefresh
        ).flatMapLatest { mostActiveCouponsResult ->
            val mostActiveCoupons = mostActiveCouponsResult.getOrThrow()

            observeCoupons(
                mostActiveCoupons.map { it.couponId },
                forceRefresh
            ).map { couponsResult ->
                couponsResult.fold(
                    onSuccess = { coupons ->
                        // Map performance reports to coupons and preserve the order of mostActiveCoupons
                        val models = mostActiveCoupons.map { performanceReport ->
                            val coupon = coupons.firstOrNull { coupon -> coupon.id == performanceReport.couponId }
                                ?: error("Coupon not found for id: ${performanceReport.couponId}")

                            CouponUiModel(
                                coupon = coupon,
                                performanceReport = performanceReport
                            )
                        }

                        Result.success(models)
                    },
                    onFailure = { Result.failure(it) }
                )
            }
        }.catch {
            WooLog.e(WooLog.T.DASHBOARD, "Failure while observing coupons", it)
            emit(Result.failure(it))
        }

    private fun observeMostActiveCoupons(
        dateRange: StatsTimeRange,
        forceRefresh: Boolean
    ) = flow {
        if (!forceRefresh && couponsReportCache?.first == dateRange) {
            val (_, cachedCoupons) = couponsReportCache
            emit(Result.success(cachedCoupons))
        } else {
            emit(
                couponRepository.fetchMostActiveCoupons(
                    dateRange = dateRange,
                    limit = COUPONS_LIMIT
                )
            )
        }
    }

    private fun observeCoupons(
        couponIds: List<Long>,
        forceRefresh: Boolean
    ) = flow {
        suspend fun fetchCoupons() = couponRepository.fetchCoupons(
            page = 1,
            pageSize = COUPONS_LIMIT,
            couponIds = couponIds
        )

        val fetchCouponsBeforeEmitting = forceRefresh ||
            couponRepository.getCoupons(couponIds).size != couponIds.size

        if (fetchCouponsBeforeEmitting) {
            fetchCoupons().onFailure {
                emit(Result.failure(it))
                return@flow
            }
        }

        emitAll(
            couponRepository.observeCoupons(couponIds)
                .map { Result.success(it) }
                .onStart { if (!fetchCouponsBeforeEmitting) fetchCoupons() }
        )
    }

    sealed interface State {
        data object Loading : State
        data class Loaded(val coupons: List<CouponUiModel>) : State
        enum class Error : State {
            Generic,
            WCAdminInactive
        }
    }

    data class CouponUiModel(
        private val coupon: Coupon,
        private val performanceReport: CouponPerformanceReport
    ) {
        val code: String = coupon.code.orEmpty()
    }

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardCouponsViewModel
    }
}