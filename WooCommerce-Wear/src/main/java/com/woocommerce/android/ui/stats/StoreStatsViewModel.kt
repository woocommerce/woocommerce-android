package com.woocommerce.android.ui.stats

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.system.NetworkStatus
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.stats.datasource.FetchStatsFromPhone
import com.woocommerce.android.ui.stats.datasource.FetchStatsFromStore
import com.woocommerce.android.ui.stats.datasource.StoreStatsRequest
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
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
    private val phoneRepository: PhoneConnectionRepository,
    private val fetchStatsFromStore: FetchStatsFromStore,
    private val fetchStatsFromPhone: FetchStatsFromPhone,
    private val networkStatus: NetworkStatus,
    loginRepository: LoginRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    private val friendlyTimeFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }

    init {
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach {
                updateSiteData(it)
                requestStoreStats(it)
            }.launchIn(this)
    }

    private suspend fun evaluateStatsSource(selectedSite: SiteModel) = when {
        networkStatus.isConnected() -> fetchStatsFromStore(selectedSite)
        phoneRepository.isPhoneConnectionAvailable() -> fetchStatsFromPhone()
        else -> error("No connection available")
    }

    private fun requestStoreStats(selectedSite: SiteModel) {
        _viewState.update { it.copy(isLoading = true) }
        launch {
            evaluateStatsSource(selectedSite)
                .onEach { handleStatsDataChange(it) }
                .launchIn(this)
        }
    }

    private fun handleStatsDataChange(statsData: StoreStatsRequest?) {
        when (statsData) {
            is StoreStatsRequest.Data -> {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        revenueTotal = statsData.revenue,
                        ordersCount = statsData.ordersCount,
                        visitorsCount = statsData.visitorsCount,
                        conversionRate = statsData.conversionRate,
                        timestamp = friendlyTimeFormat.format(Date())
                    )
                }
            }

            else -> _viewState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateSiteData(site: SiteModel) {
        _viewState.update { it.copy(currentSiteName = site.name) }
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
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
