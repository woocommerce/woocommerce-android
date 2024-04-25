package com.woocommerce.android.ui.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenEditWidgets
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    dashboardTransactionLauncher: DashboardTransactionLauncher,
    shouldShowPrivacyBanner: ShouldShowPrivacyBanner,
    private val dashboardRepository: DashboardRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
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

    private var _appbarState = MutableLiveData<AppbarState>()
    val appbarState: LiveData<AppbarState> = _appbarState

    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    val refreshTrigger: Flow<RefreshEvent> = _refreshTrigger.asSharedFlow()

    val storeName = selectedSite.observe().map { site ->
        if (!site?.displayName.isNullOrBlank()) {
            site?.displayName
        } else {
            site?.name
        } ?: resourceProvider.getString(R.string.store_name_default)
    }.asLiveData()

    val jetpackBenefitsBannerState = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest { site ->
            jetpackBenefitsBannerState(site.connectionType)
        }.asLiveData()

    val dashboardWidgets = dashboardRepository.widgets.asLiveData()

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
            _refreshTrigger.tryEmit(RefreshEvent())
        }
    }

    fun onPullToRefresh() {
        usageTracksEventEmitter.interacted()
        analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_PULLED_TO_REFRESH)
        _refreshTrigger.tryEmit(RefreshEvent(isForced = true))
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

    fun onDashboardWidgetEvent(event: DashboardEvent) {
        triggerEvent(event)
    }

    private fun jetpackBenefitsBannerState(
        connectionType: SiteConnectionType
    ): Flow<JetpackBenefitsBannerUiModel?> {
        if (connectionType == SiteConnectionType.Jetpack) {
            return flowOf(null)
        }

        val dismissTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        return dismissTrigger.onStart { emit(Unit) }
            .map {
                val durationSinceDismissal =
                    (System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()).milliseconds
                val showBanner = durationSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER.days
                JetpackBenefitsBannerUiModel(
                    show = showBanner,
                    onDismiss = {
                        appPrefsWrapper.recordJetpackBenefitsDismissal()
                        analyticsTrackerWrapper.track(
                            stat = FEATURE_JETPACK_BENEFITS_BANNER,
                            properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
                        )
                        dismissTrigger.tryEmit(Unit)
                    }
                )
            }
    }

    sealed class OrderState {
        data object Empty : OrderState()
        data object AtLeastOne : OrderState()
    }

    data class AppbarState(
        val showShareStoreButton: Boolean = false,
    )

    sealed class DashboardEvent : MultiLiveEvent.Event() {

        data object ShowPrivacyBanner : DashboardEvent()

        data object ShowAIProductDescriptionDialog : DashboardEvent()

        data class ShareStore(val storeUrl: String) : DashboardEvent()

        data object OpenEditWidgets : DashboardEvent()

        data object ShowStatsError : DashboardEvent()

        data class OpenRangePicker(
            val start: Long,
            val end: Long,
            val callback: (Long, Long) -> Unit
        ) : DashboardEvent()

        data object ShowPluginUnavailableError : DashboardEvent()

        data object NavigateToAddProduct : DashboardEvent()
    }

    data class RefreshEvent(val isForced: Boolean = false)

    data class JetpackBenefitsBannerUiModel(
        val show: Boolean = false,
        val onDismiss: () -> Unit = {}
    )

    data class DashboardWidgetMenu(
        val items: List<DashboardWidgetAction>
    )

    data class DashboardWidgetAction(
        @StringRes val titleResource: Int,
        val action: () -> Unit
    )
}
