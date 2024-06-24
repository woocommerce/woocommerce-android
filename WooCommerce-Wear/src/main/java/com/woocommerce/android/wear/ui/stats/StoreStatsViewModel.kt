package com.woocommerce.android.wear.ui.stats

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.extensions.getStateFlow
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Error
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Finished
import com.woocommerce.android.wear.viewmodel.WearViewModel
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STATS_DATA_FAILED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STATS_DATA_REQUESTED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STATS_DATA_SUCCEEDED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StoreStatsViewModel @Inject constructor(
    private val fetchStats: FetchStats,
    private val locale: Locale,
    private val loginRepository: LoginRepository,
    private val analyticsTracker: AnalyticsTracker,
    savedState: SavedStateHandle
) : WearViewModel() {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    private val friendlyTimeFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("h:mm a", locale)
    }

    init {
        _viewState.update { it.copy(isLoading = true) }
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach { site ->
                _viewState.update { it.copy(isLoading = true) }
                updateSiteData(site)
                requestStoreStats(site)
            }.launchIn(this)
    }

    override fun reloadData(withLoading: Boolean) {
        if (_viewState.value.isLoading) return
        _viewState.update { it.copy(isLoading = withLoading) }
        launch {
            loginRepository.selectedSite?.let {
                updateSiteData(it)
                requestStoreStats(it)
            }
        }
    }

    private fun requestStoreStats(selectedSite: SiteModel) {
        analyticsTracker.track(WATCH_STATS_DATA_REQUESTED)
        launch {
            fetchStats(selectedSite)
                .onEach { handleStatsDataChange(it) }
                .launchIn(this)
        }
    }

    private fun handleStatsDataChange(request: StoreStatsRequest?) {
        when (request) {
            is Finished -> {
                analyticsTracker.track(WATCH_STATS_DATA_SUCCEEDED)
                val statsData = request.data
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        isError = false,
                        revenueTotal = statsData.revenue,
                        ordersCount = statsData.ordersCount,
                        visitorsCount = statsData.visitorsCount,
                        conversionRate = statsData.conversionRate,
                        timestamp = friendlyTimeFormat.format(Date())
                    )
                }
            }
            is Error -> {
                analyticsTracker.track(WATCH_STATS_DATA_FAILED)
                _viewState.update { it.copy(isLoading = false, isError = true) }
            }
            else -> _viewState.update { it.copy(isLoading = true, isError = false) }
        }
    }

    private fun updateSiteData(site: SiteModel) {
        _viewState.update { it.copy(currentSiteName = site.name) }
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val currentSiteName: String? = null,
        val revenueTotal: String? = null,
        val ordersCount: Int? = null,
        val visitorsCount: Int? = null,
        val conversionRate: String? = null,
        val timestamp: String? = null
    ) : Parcelable

    @Parcelize
    data class StoreData(
        val title: String,
        val value: String,
    ) : Parcelable
}
