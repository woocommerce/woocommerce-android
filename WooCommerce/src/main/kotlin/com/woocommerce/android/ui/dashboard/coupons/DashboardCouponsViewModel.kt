package com.woocommerce.android.ui.dashboard.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    val viewState = refreshTrigger
        .onStart { emit(RefreshEvent()) }
        .transform {
            emit(State.Loading)
            emitAll(observeMostActiveCoupons(it.isForced).map { result ->
                result.fold(
                    onSuccess = { coupons -> State.Loaded(coupons) },
                    onFailure = { State.Error.Generic }
                )
            })
        }
        .asLiveData()

    private fun observeMostActiveCoupons(forceRefresh: Boolean): Flow<Result<List<CouponUiModel>>> = flow {
        val mostActiveCoupons = fetchMostActiveCoupons(
            dateRange = SelectionType.MONTH_TO_DATE.generateSelectionData( // TODO pass date range
                referenceStartDate = Date(),
                referenceEndDate = Date(),
                calendar = Calendar.getInstance(),
                locale = Locale.getDefault()
            ).currentRange
        ).getOrElse {
            emit(Result.failure(it))
            return@flow
        }

        val couponIds = mostActiveCoupons.map { it.couponId }

        val fetchCouponsBeforeEmitting = forceRefresh ||
            couponRepository.getCoupons(couponIds).size != mostActiveCoupons.size

        if (fetchCouponsBeforeEmitting) {
            fetchCoupons(couponIds).onFailure {
                emit(Result.failure(it))
                return@flow
            }
        }

        emitAll(
            couponRepository.observeCoupons(couponIds)
                .map { coupons ->
                    Result.success(
                        mostActiveCoupons.map { performanceReport ->
                            val coupon = coupons.firstOrNull { coupon -> coupon.id == performanceReport.couponId }
                                ?: error("Coupon not found for id: ${performanceReport.couponId}")

                            CouponUiModel(
                                code = coupon.code.orEmpty()
                            )
                        }
                    )
                }
                .catch { emit(Result.failure(it)) }
                .onStart {
                    if (!fetchCouponsBeforeEmitting) {
                        fetchCoupons(couponIds)
                    }
                }
        )
    }

    private suspend fun fetchMostActiveCoupons(dateRange: StatsTimeRange) = couponRepository.fetchMostActiveCoupons(
        dateRange = dateRange,
        limit = COUPONS_LIMIT
    )

    private suspend fun fetchCoupons(couponIds: List<Long>) = couponRepository.fetchCoupons(
        page = 1,
        pageSize = COUPONS_LIMIT,
        couponIds = couponIds
    )

    sealed interface State {
        data object Loading : State
        data class Loaded(val coupons: List<CouponUiModel>) : State
        enum class Error : State {
            Generic,
            WCAdminInactive
        }
    }

    data class CouponUiModel(
        val code: String,
    )

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardCouponsViewModel
    }
}
