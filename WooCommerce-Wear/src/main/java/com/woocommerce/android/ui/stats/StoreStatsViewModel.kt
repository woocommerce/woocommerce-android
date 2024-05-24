package com.woocommerce.android.ui.stats

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.extensions.getStateFlow
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.stats.datasource.FetchStats
import com.woocommerce.android.ui.stats.datasource.FetchStats.StoreStatsRequest
import com.woocommerce.android.ui.stats.datasource.FetchStats.StoreStatsRequest.Finished
import com.woocommerce.android.ui.stats.datasource.FetchStats.StoreStatsRequest.Waiting
import com.woocommerce.android.viewmodel.WearViewModel
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
            .onEach {
                updateSiteData(it)
                requestStoreStats(it)
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
        launch {
            fetchStats(selectedSite)
                .onEach { handleStatsDataChange(it) }
                .launchIn(this)
        }
    }

    private fun handleStatsDataChange(request: StoreStatsRequest?) {
        when (request) {
            is Finished -> {
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
            is Waiting -> _viewState.update { it.copy(isLoading = true, isError = false) }
            else -> _viewState.update { it.copy(isLoading = false, isError = true) }
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
