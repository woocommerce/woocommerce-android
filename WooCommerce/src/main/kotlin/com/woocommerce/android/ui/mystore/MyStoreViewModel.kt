package com.woocommerce.android.ui.mystore

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.HasOrders
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.IsJetPackCPEnabled
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.PluginNotActive
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.RevenueStatsError
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.RevenueStatsSuccess
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.VisitorsStatsError
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.VisitorsStatsSuccess
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.ui.payments.banner.BannerState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.UtmProvider
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.store.JitmStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MyStoreViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val wooCommerceStore: WooCommerceStore,
    private val getStats: GetStats,
    private val getTopPerformers: GetTopPerformers,
    private val currencyFormatter: CurrencyFormatter,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val myStoreTransactionLauncher: MyStoreTransactionLauncher,
    private val jitmStore: JitmStore,
    @Named("my-store") private val utmProvider: UtmProvider,
) : ScopedViewModel(savedState) {
    companion object {
        const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
        const val JITM_MESSAGE_PATH = "woomobile:my_store:admin_notices"
        const val UTM_CAMPAIGN = "jitm_group_woomobile_ipp"
        const val UTM_SOURCE = "my_store"
        const val UTM_CONTENT = "jitm_woomobile_ipp_barcode_users"
    }

    val performanceObserver: LifecycleObserver = myStoreTransactionLauncher

    private var _revenueStatsState = MutableLiveData<RevenueStatsViewState>()
    val revenueStatsState: LiveData<RevenueStatsViewState> = _revenueStatsState

    private var _visitorStatsState = MutableLiveData<VisitorStatsViewState>()
    val visitorStatsState: LiveData<VisitorStatsViewState> = _visitorStatsState

    private var _topPerformersState = MutableLiveData<TopPerformersState>()
    val topPerformersState: LiveData<TopPerformersState> = _topPerformersState

    private val _bannerState: MutableLiveData<BannerState> = MutableLiveData()
    val bannerState: LiveData<BannerState> = _bannerState

    private var _hasOrders = MutableLiveData<OrderState>()
    val hasOrders: LiveData<OrderState> = _hasOrders

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _activeStatsGranularity = savedState.getStateFlow(viewModelScope, getSelectedStatsGranularityIfAny())
    val activeStatsGranularity = _activeStatsGranularity.asLiveData()

    @VisibleForTesting
    val refreshStoreStats = BooleanArray(StatsGranularity.values().size) { true }

    @VisibleForTesting
    val refreshTopPerformerStats = BooleanArray(StatsGranularity.values().size) { true }

    private var jetpackMonitoringJob: Job? = null

    init {
        ConnectionChangeReceiver.getEventBus().register(this)

        _topPerformersState.value = TopPerformersState(isLoading = true)

        viewModelScope.launch {
            if (isCountryUSA()) {
                val response = jitmStore.fetchJitmMessage(selectedSite.get(), JITM_MESSAGE_PATH)
                populateResultToUI(response)
            }
        }
        viewModelScope.launch {
            combine(
                _activeStatsGranularity,
                refreshTrigger.onStart { emit(Unit) }
            ) { granularity, _ ->
                granularity
            }.collectLatest { granularity ->
                coroutineScope {
                    launch { loadStoreStats(granularity) }
                    launch { loadTopPerformersStats(granularity) }
                }
            }
        }
        observeTopPerformerUpdates()
    }

    private fun isCountryUSA(): Boolean {
        return getStoreCountryCode().equals("US", ignoreCase = true)
    }

    private fun getStoreCountryCode(): String? {
        return wooCommerceStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
            WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
        }
    }

    private fun populateResultToUI(response: WooResult<Array<JITMApiResponse>>) {
        when {
            response.isError -> {}
            !response.model.isNullOrEmpty() -> {
                trackJitmFetchSuccessEvent(
                    response.model!!.map {
                        it.id
                    }
                )
                _bannerState.value = BannerState(
                    shouldDisplayBanner = true,
                    onPrimaryActionClicked = { onJitmCtaClicked(response.model!![0].cta.link) },
                    onDismissClicked = {
                        onJitmDismissClicked(
                            response.model!![0].id,
                            response.model!![0].featureClass
                        )
                    },
                    title = UiString.UiStringText(response.model!![0].content.message),
                    description = UiString.UiStringText(response.model!![0].content.description),
                    primaryActionLabel = UiString.UiStringText(response.model!![0].cta.message),
                    chipLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
                )
            }
            !response.isError && response.model.isNullOrEmpty() -> {
                // JITM fetch api succeeded but there aren't any JITMs to display at the moment
                trackJitmFetchSuccessEvent(emptyList())
            }
        }
    }

    private fun trackJitmFetchSuccessEvent(jitmIdList: List<String>) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.JITM_FETCH_SUCCESS,
            mapOf(
                KEY_SOURCE to UTM_SOURCE,
                "jitms" to jitmIdList
            )
        )
    }

    private fun onJitmCtaClicked(url: String) {
        triggerEvent(
            MyStoreEvent.OnJitmCtaClicked(
                utmProvider.getUrlWithUtmParams(url)
            )
        )
    }

    private fun onJitmDismissClicked(jitmId: String, featureClass: String) {
        viewModelScope.launch {
            jitmStore.dismissJitmMessage(selectedSite.get(), jitmId, featureClass)
        }
        triggerEvent(MyStoreEvent.OnJitmDismissed)
    }

    override fun onCleared() {
        ConnectionChangeReceiver.getEventBus().unregister(this)
        super.onCleared()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            if (refreshStoreStats.any { it } || refreshTopPerformerStats.any { it }) {
                refreshTrigger.tryEmit(Unit)
            }
        }
    }

    fun onStatsGranularityChanged(granularity: StatsGranularity) {
        usageTracksEventEmitter.interacted()
        _activeStatsGranularity.update { granularity }
        launch {
            appPrefsWrapper.setActiveStatsGranularity(selectedSite.getSelectedSiteId(), granularity.name)
        }
    }

    fun onSwipeToRefresh() {
        usageTracksEventEmitter.interacted()
        analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_PULLED_TO_REFRESH)
        resetForceRefresh()
        refreshTrigger.tryEmit(Unit)
    }

    fun getSelectedSiteName(): String =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        } ?: ""

    private suspend fun loadStoreStats(granularity: StatsGranularity) {
        if (!networkStatus.isConnected()) {
            refreshStoreStats[granularity.ordinal] = true
            _revenueStatsState.value = RevenueStatsViewState.Content(null, granularity)
            _visitorStatsState.value = VisitorStatsViewState.Content(emptyMap())
            return
        }

        val forceRefresh = refreshStoreStats[granularity.ordinal]
        if (forceRefresh) {
            refreshStoreStats[granularity.ordinal] = false
        }
        _revenueStatsState.value = RevenueStatsViewState.Loading
        getStats(forceRefresh, granularity)
            .collect {
                when (it) {
                    is RevenueStatsSuccess -> onRevenueStatsSuccess(it, granularity)
                    is RevenueStatsError -> _revenueStatsState.value = RevenueStatsViewState.GenericError
                    PluginNotActive -> _revenueStatsState.value = RevenueStatsViewState.PluginNotActiveError
                    is VisitorsStatsSuccess -> _visitorStatsState.value = VisitorStatsViewState.Content(it.stats)
                    is VisitorsStatsError -> _visitorStatsState.value = VisitorStatsViewState.Error
                    IsJetPackCPEnabled -> onJetPackCpConnected()
                    is HasOrders -> _hasOrders.value = if (it.hasOrder) OrderState.AtLeastOne else OrderState.Empty
                }
                myStoreTransactionLauncher.onStoreStatisticsFetched()
            }
    }

    private fun onRevenueStatsSuccess(
        it: RevenueStatsSuccess,
        selectedGranularity: StatsGranularity
    ) {
        _revenueStatsState.value = RevenueStatsViewState.Content(
            it.stats?.toStoreStatsUiModel(),
            selectedGranularity
        )
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DASHBOARD_MAIN_STATS_LOADED,
            mapOf(AnalyticsTracker.KEY_RANGE to selectedGranularity.name.lowercase())
        )
    }

    private fun onJetPackCpConnected() {
        val daysSinceDismissal = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - appPrefsWrapper.getJetpackBenefitsDismissalDate()
        )
        val showBanner = daysSinceDismissal >= DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER
        val benefitsBanner =
            BenefitsBannerUiModel(
                show = showBanner,
                onDismiss = {
                    _visitorStatsState.value =
                        VisitorStatsViewState.JetpackCpConnected(BenefitsBannerUiModel(show = false))
                    appPrefsWrapper.recordJetpackBenefitsDismissal()
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
                        properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "dismissed")
                    )
                }
            )
        _visitorStatsState.value = VisitorStatsViewState.JetpackCpConnected(benefitsBanner)
        monitorJetpackInstallation()
    }

    private fun monitorJetpackInstallation() {
        jetpackMonitoringJob?.cancel()
        jetpackMonitoringJob = viewModelScope.launch {
            selectedSite.observe()
                .filter { it?.isJetpackConnected == true }
                .take(1)
                .collect {
                    loadStoreStats(_activeStatsGranularity.value)
                }
        }
    }

    private suspend fun loadTopPerformersStats(granularity: StatsGranularity) {
        if (!networkStatus.isConnected()) {
            refreshTopPerformerStats[granularity.ordinal] = true
            return
        }

        val forceRefresh = refreshTopPerformerStats[granularity.ordinal]
        if (forceRefresh) {
            refreshTopPerformerStats[granularity.ordinal] = false
        }

        _topPerformersState.value = _topPerformersState.value?.copy(isLoading = true, isError = false)
        val result = getTopPerformers.fetchTopPerformers(granularity, forceRefresh)
        result.fold(
            onFailure = { _topPerformersState.value = _topPerformersState.value?.copy(isError = true) },
            onSuccess = {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
                )
            }
        )
        _topPerformersState.value = _topPerformersState.value?.copy(isLoading = false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTopPerformerUpdates() {
        viewModelScope.launch {
            _activeStatsGranularity
                .flatMapLatest { granularity -> getTopPerformers.observeTopPerformers(granularity) }
                .collectLatest {
                    _topPerformersState.value = _topPerformersState.value?.copy(
                        topPerformers = it.toTopPerformersUiList(),
                    )
                }
        }
    }

    private fun onTopPerformerSelected(productId: Long) {
        triggerEvent(MyStoreEvent.OpenTopPerformer(productId))
        analyticsTrackerWrapper.track(AnalyticsEvent.TOP_EARNER_PRODUCT_TAPPED)
        usageTracksEventEmitter.interacted()
    }

    private fun WCRevenueStatsModel.toStoreStatsUiModel(): RevenueStatsUiModel {
        val totals = parseTotal()
        return RevenueStatsUiModel(
            intervalList = getIntervalList().toStatsIntervalUiModelList(),
            totalOrdersCount = totals?.ordersCount,
            totalSales = totals?.totalSales,
            currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        )
    }

    private fun List<WCRevenueStatsModel.Interval>.toStatsIntervalUiModelList() =
        map {
            StatsIntervalUiModel(
                it.interval,
                it.subtotals?.ordersCount,
                it.subtotals?.totalSales
            )
        }

    private fun List<TopPerformerProduct>.toTopPerformersUiList() = map { it.toTopPerformersUiModel() }

    private fun TopPerformerProduct.toTopPerformersUiModel() =
        TopPerformerProductUiModel(
            productId = productId,
            name = StringEscapeUtils.unescapeHtml4(name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            netSales = resourceProvider.getString(
                R.string.dashboard_top_performers_net_sales,
                getTotalSpendFormatted(total.toBigDecimal(), currency)
            ),
            imageUrl = imageUrl?.toImageUrl(),
            onClick = ::onTopPerformerSelected
        )

    private fun resetForceRefresh() {
        refreshTopPerformerStats.forEachIndexed { index, _ ->
            refreshTopPerformerStats[index] = true
        }
        refreshStoreStats.forEachIndexed { index, _ ->
            refreshStoreStats[index] = true
        }
    }

    private fun getTotalSpendFormatted(totalSpend: BigDecimal, currency: String) =
        currencyFormatter.formatCurrency(
            totalSpend,
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: currency
        )

    private fun String.toImageUrl() =
        PhotonUtils.getPhotonImageUrl(
            this,
            resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100),
            0
        )

    private fun getSelectedStatsGranularityIfAny(): StatsGranularity {
        val previouslySelectedGranularity = appPrefsWrapper.getActiveStatsGranularity(selectedSite.getSelectedSiteId())
        return runCatching { StatsGranularity.valueOf(previouslySelectedGranularity.uppercase()) }
            .getOrElse { StatsGranularity.DAYS }
    }

    sealed class RevenueStatsViewState {
        object Loading : RevenueStatsViewState()
        object GenericError : RevenueStatsViewState()
        object PluginNotActiveError : RevenueStatsViewState()
        data class Content(
            val revenueStats: RevenueStatsUiModel?,
            val granularity: StatsGranularity
        ) : RevenueStatsViewState()
    }

    sealed class VisitorStatsViewState {
        object Error : VisitorStatsViewState()
        data class JetpackCpConnected(
            val benefitsBanner: BenefitsBannerUiModel
        ) : VisitorStatsViewState()

        data class Content(
            val stats: Map<String, Int>
        ) : VisitorStatsViewState()
    }

    data class TopPerformersState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val topPerformers: List<TopPerformerProductUiModel> = emptyList(),
    )

    sealed class OrderState {
        object Empty : OrderState()
        object AtLeastOne : OrderState()
    }

    sealed class MyStoreEvent : MultiLiveEvent.Event() {
        data class OpenTopPerformer(
            val productId: Long
        ) : MyStoreEvent()

        data class OnJitmCtaClicked(
            val url: String,
            @StringRes val titleRes: Int = R.string.card_reader_purchase_card_reader
        ) : MyStoreEvent()

        object OnJitmDismissed : MyStoreEvent()
    }
}
