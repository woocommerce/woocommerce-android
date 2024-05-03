package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.mystore.stats.StatsRepository
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel

@Suppress("UnusedPrivateProperty")
@HiltViewModel(assistedFactory = MyStoreViewModel.Factory::class)
class MyStoreViewModel @AssistedInject constructor(
    private val statsRepository: StatsRepository,
    @Assisted private val navController: NavHostController,
    loginRepository: LoginRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach {
                updateSiteData(it)
                requestStoreStats(it)
            }.launchIn(this)
    }

    private fun requestStoreStats(selectedSite: SiteModel) {
        launch {
            statsRepository.fetchRevenueStats(selectedSite)
                .fold(
                    onSuccess = { updateRevenueStats(it) },
                    onFailure = { /* Handle error */ }
                )
        }
    }

    private fun updateRevenueStats(stats: WCRevenueStatsModel?) {
        _viewState.update {
            it.copy(todayRevenueTotal = stats?.parseTotal()?.netRevenue)
        }
    }

    private fun updateSiteData(site: SiteModel) {
        _viewState.update {
            it.copy(
                currentSiteName = site.name
            )
        }
    }

    @Parcelize
    data class ViewState(
        val currentSiteName: String? = null,
        val todayRevenueTotal: Double? = null
    ) : Parcelable

    @Parcelize
    data class StoreData(
        val title: String,
        val value: String,
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): MyStoreViewModel
    }
}
