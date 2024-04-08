package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenEditWidgets
import com.woocommerce.android.ui.dashboard.stats.GetSelectedDateRange
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    dashboardTransactionLauncher: DashboardTransactionLauncher,
    getSelectedDateRange: GetSelectedDateRange,
    shouldShowPrivacyBanner: ShouldShowPrivacyBanner
) : ScopedViewModel(savedState) {
    companion object {
        val SUPPORTED_RANGES_ON_MY_STORE_TAB = listOf(
            SelectionType.TODAY,
            SelectionType.WEEK_TO_DATE,
            SelectionType.MONTH_TO_DATE,
            SelectionType.YEAR_TO_DATE,
            SelectionType.CUSTOM
        )
    }

    val performanceObserver: LifecycleObserver = dashboardTransactionLauncher

    private var _hasOrders = MutableLiveData<OrderState>()
    val hasOrders: LiveData<OrderState> = _hasOrders

    private var _lastUpdateTopPerformers = MutableLiveData<Long?>()
    val lastUpdateTopPerformers: LiveData<Long?> = _lastUpdateTopPerformers

    private var _appbarState = MutableLiveData<AppbarState>()
    val appbarState: LiveData<AppbarState> = _appbarState

    private val refreshTrigger = MutableSharedFlow<RefreshState>(extraBufferCapacity = 1)

    private val _selectedDateRange = getSelectedDateRange()
    val selectedDateRange: LiveData<StatsTimeRangeSelection> = _selectedDateRange.asLiveData()

    val storeName = selectedSite.observe().map { site ->
        if (!site?.displayName.isNullOrBlank()) {
            site?.displayName
        } else {
            site?.name
        } ?: resourceProvider.getString(R.string.store_name_default)
    }.asLiveData()

    init {
        ConnectionChangeReceiver.getEventBus().register(this)

        launch {
            shouldShowPrivacyBanner().let {
                if (it) {
                    triggerEvent(DashboardEvent.ShowPrivacyBanner)
                }
            }
        }

        if (selectedSite.getOrNull()?.isEligibleForAI == true &&
            !appPrefsWrapper.wasAIProductDescriptionPromoDialogShown
        ) {
            triggerEvent(DashboardEvent.ShowAIProductDescriptionDialog)
            appPrefsWrapper.wasAIProductDescriptionPromoDialogShown = true
        }

        updateShareStoreButtonVisibility()
    }

    private fun updateShareStoreButtonVisibility() {
        _appbarState.value = AppbarState(showShareStoreButton = selectedSite.get().isSitePublic)
    }

    override fun onCleared() {
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.onCleared()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            refreshTrigger.tryEmit(RefreshState())
        }
    }

    fun onPullToRefresh() {
        usageTracksEventEmitter.interacted()
        analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_PULLED_TO_REFRESH)
        refreshTrigger.tryEmit(RefreshState(isForced = true))
    }

    fun onShareStoreClicked() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
        triggerEvent(
            DashboardEvent.ShareStore(storeUrl = selectedSite.get().url)
        )
    }

    fun onEditWidgetsClicked() {
        // TODO ADD TRACKING HERE
        triggerEvent(OpenEditWidgets)
    }

    sealed class OrderState {
        data object Empty : OrderState()
        data object AtLeastOne : OrderState()
    }

    data class AppbarState(
        val showShareStoreButton: Boolean = false,
    )

    sealed class DashboardEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : DashboardEvent()

        data object ShowPrivacyBanner : DashboardEvent()

        data object ShowAIProductDescriptionDialog : DashboardEvent()

        data class ShareStore(val storeUrl: String) : DashboardEvent()

        data object OpenEditWidgets : DashboardEvent()
    }

    data class RefreshState(private val isForced: Boolean = false) {
        /**
         * [shouldRefresh] will be true only the first time the refresh event is consulted and when
         * isForced is initialized on true. Once the event is handled the property will change its value to false
         */
        var shouldRefresh: Boolean = isForced
            private set
            get(): Boolean {
                val result = field
                if (field) {
                    field = false
                }
                return result
            }
    }
}
