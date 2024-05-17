package com.woocommerce.android.ui.dashboard.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.WooException
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.CouponsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DashboardCouponsViewModel.Factory::class)
@Suppress("LongParameterList")
class DashboardCouponsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val couponRepository: CouponRepository,
    getSelectedRange: GetSelectedRangeForCoupons,
    private val customDateRangeDataStore: CouponsCustomDateRangeDataStore,
    private val dateRangeFormatter: DashboardDateRangeFormatter,
    private val appPrefs: AppPrefsWrapper,
    private val couponUtils: CouponUtils,
    private val parameterRepository: ParameterRepository,
    private val coroutineDispatchers: CoroutineDispatchers
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val COUPONS_LIMIT = 3
    }

    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)

    private var couponsReportCache = mutableMapOf<StatsTimeRange, List<CouponPerformanceReport>>()

    private val selectedDateRange = getSelectedRange()
        .shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val currencyCodeTask = async(coroutineDispatchers.io) {
        parameterRepository.getParameters().currencyCode
    }

    val dateRangeState = combine(
        selectedDateRange,
        customDateRangeDataStore.dateRange
    ) { rangeSelection, customRange ->
        DateRangeState(
            rangeSelection = rangeSelection,
            customRange = customRange,
            rangeFormatted = dateRangeFormatter.formatRangeDate(rangeSelection)
        )
    }.asLiveData()

    val viewState = selectedDateRange.flatMapLatest { rangeSelection ->
        refreshTrigger
            .onStart { emit(RefreshEvent()) }
            .transformLatest {
                if (it.isForced || !couponsReportCache.containsKey(rangeSelection.currentRange)) {
                    emit(State.Loading)
                }
                emitAll(
                    observeCouponUiModels(rangeSelection.currentRange, it.isForced).map { result ->
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
    }.asLiveData()

    fun onTabSelected(selectionType: SelectionType) {
        if (selectionType != SelectionType.CUSTOM) {
            appPrefs.setActiveCouponsTab(selectionType.name)
        } else {
            if (dateRangeState.value?.customRange == null) {
                onEditCustomRangeTapped()
            } else {
                appPrefs.setActiveTopPerformersTab(SelectionType.CUSTOM.name)
            }
        }
    }

    fun onEditCustomRangeTapped() {
        triggerEvent(
            OpenDatePicker(
                fromDate = dateRangeState.value?.customRange?.start ?: Date(),
                toDate = dateRangeState.value?.customRange?.end ?: Date()
            )
        )
    }

    fun onCustomRangeSelected(range: StatsTimeRange) {
        viewModelScope.launch {
            customDateRangeDataStore.updateDateRange(range)
            if (dateRangeState.value?.rangeSelection?.selectionType != SelectionType.CUSTOM) {
                onTabSelected(SelectionType.CUSTOM)
            }
        }
    }

    fun onViewAllClicked() {
        triggerEvent(ViewAllCoupons)
    }

    fun onRetryClicked() {
        _refreshTrigger.tryEmit(RefreshEvent())
    }

    private fun observeCouponUiModels(
        dateRange: StatsTimeRange,
        forceRefresh: Boolean
    ): Flow<Result<List<CouponUiModel>>> =
        observeMostActiveCoupons(
            dateRange = dateRange,
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
                                code = coupon.code.orEmpty(),
                                uses = performanceReport.ordersCount,
                                description = couponUtils.generateSummary(coupon, currencyCodeTask.await())
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
        if (!forceRefresh && couponsReportCache.containsKey(dateRange)) {
            emit(Result.success(couponsReportCache.getValue(dateRange)))
        } else {
            emit(
                couponRepository.fetchMostActiveCoupons(
                    dateRange = dateRange,
                    limit = COUPONS_LIMIT
                ).onSuccess {
                    couponsReportCache[dateRange] = it
                }
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
                .onStart {
                    if (!fetchCouponsBeforeEmitting) {
                        launch { fetchCoupons() }
                    }
                }
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

    data class DateRangeState(
        val rangeSelection: StatsTimeRangeSelection,
        val customRange: StatsTimeRange?,
        val rangeFormatted: String
    )

    data class CouponUiModel(
        val code: String,
        val uses: Int,
        val description: String
    )

    data class OpenDatePicker(val fromDate: Date, val toDate: Date) : MultiLiveEvent.Event()
    data object ViewAllCoupons : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardCouponsViewModel
    }
}
