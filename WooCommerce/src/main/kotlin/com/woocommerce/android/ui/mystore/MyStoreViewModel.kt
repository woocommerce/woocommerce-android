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
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod
import com.woocommerce.android.ui.jitm.JitmTracker
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
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
    private val jitmTracker: JitmTracker,
    private val myStoreUtmProvider: MyStoreUtmProvider,
) : ScopedViewModel(savedState) {
    companion object {
        private const val DAYS_TO_REDISPLAY_JP_BENEFITS_BANNER = 5
        private const val JITM_MESSAGE_PATH = "woomobile:my_store:admin_notices"
        const val UTM_SOURCE = "my_store"
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
            val response = jitmStore.fetchJitmMessage(
                selectedSite.get(),
                JITM_MESSAGE_PATH,
                getEncodedQueryParams(),
            )
            populateResultToUI(response)
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

    private fun getEncodedQueryParams(): String {
        val query = if (BuildConfig.DEBUG) {
            "build_type=developer&platform=android&version=${BuildConfig.VERSION_NAME}"
        } else {
            "platform=android&version=${BuildConfig.VERSION_NAME}"
        }
        return URLEncoder.encode(query, Charsets.UTF_8.name())
    }

    private fun populateResultToUI(response: WooResult<Array<JITMApiResponse>>) {
        if (response.isError) {
            jitmTracker.trackJitmFetchFailure(UTM_SOURCE, response.error.type, response.error.message)
            WooLog.e(WooLog.T.JITM, "Failed to fetch JITM for the message path $JITM_MESSAGE_PATH")
            return
        }

        jitmTracker.trackJitmFetchSuccess(
            UTM_SOURCE,
            response.model?.getOrNull(0)?.id,
            response.model?.size
        )
        response.model?.getOrNull(0)?.let { model: JITMApiResponse ->
            jitmTracker.trackJitmDisplayed(
                UTM_SOURCE,
                model.id,
                model.featureClass
            )
            _bannerState.value = BannerState(
                shouldDisplayBanner = true,
                onPrimaryActionClicked = {
                    onJitmCtaClicked(
                        id = model.id,
                        featureClass = model.featureClass,
                        url = model.cta.link
                    )
                },
                onDismissClicked = {
                    onJitmDismissClicked(
                        model.id,
                        model.featureClass
                    )
                },
                title = UiString.UiStringText(model.content.message),
                description = UiString.UiStringText(model.content.description),
                primaryActionLabel = UiString.UiStringText(model.cta.message),
                chipLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
            )
        } ?: WooLog.i(WooLog.T.JITM, "No JITM Campaign in progress")
    }

    private fun onJitmCtaClicked(
        id: String,
        featureClass: String,
        url: String
    ) {
        jitmTracker.trackJitmCtaTapped(
            UTM_SOURCE,
            id,
            featureClass
        )
        triggerEvent(
            MyStoreEvent.OnJitmCtaClicked(
                myStoreUtmProvider.getUrlWithUtmParams(
                    source = UTM_SOURCE,
                    id = id,
                    featureClass = featureClass,
                    siteId = selectedSite.getIfExists()?.siteId,
                    url = url
                )
            )
        )
    }

    private fun onJitmDismissClicked(jitmId: String, featureClass: String) {
        _bannerState.value = _bannerState.value?.copy(shouldDisplayBanner = false)
        jitmTracker.trackJitmDismissTapped(UTM_SOURCE, jitmId, featureClass)
        viewModelScope.launch {
            jitmStore.dismissJitmMessage(selectedSite.get(), jitmId, featureClass).also { response ->
                when {
                    response.model != null && response.model!! -> {
                        jitmTracker.trackJitmDismissSuccess(
                            UTM_SOURCE,
                            jitmId,
                            featureClass
                        )
                    }
                    else -> jitmTracker.trackJitmDismissFailure(
                        UTM_SOURCE,
                        jitmId,
                        featureClass,
                        response.error?.type,
                        response.error?.message
                    )
                }
            }
        }
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

    fun onViewAnalyticsClicked() {
        AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SEE_MORE_ANALYTICS_TAPPED)
        val targetPeriod = when (val state = revenueStatsState.value) {
            is RevenueStatsViewState.Content -> state.granularity.toAnalyticTimePeriod()
            else -> AnalyticTimePeriod.TODAY
        }
        triggerEvent(MyStoreEvent.OpenAnalytics(targetPeriod))
    }

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

    private fun StatsGranularity.toAnalyticTimePeriod() = when (this) {
        StatsGranularity.DAYS -> AnalyticTimePeriod.TODAY
        StatsGranularity.WEEKS -> AnalyticTimePeriod.WEEK_TO_DATE
        StatsGranularity.MONTHS -> AnalyticTimePeriod.MONTH_TO_DATE
        StatsGranularity.YEARS -> AnalyticTimePeriod.YEAR_TO_DATE
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

        data class OpenAnalytics(val analyticsPeriod: AnalyticTimePeriod) : MyStoreEvent()
        data class OnJitmCtaClicked(
            val url: String,
            @StringRes val titleRes: Int = R.string.card_reader_purchase_card_reader
        ) : MyStoreEvent()
    }
}
