package com.woocommerce.android.ui.login

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.getStateFlow
import com.woocommerce.android.ui.NavRoutes
import com.woocommerce.android.ui.NavRoutes.MY_STORE
import com.woocommerce.android.ui.login.FetchSiteData.LoginRequestState.Logged
import com.woocommerce.android.ui.login.FetchSiteData.LoginRequestState.Timeout
import com.woocommerce.android.ui.login.FetchSiteData.LoginRequestState.Waiting
import com.woocommerce.android.viewmodel.WearViewModel
import com.woocommerce.commons.WearAnalyticsEvent
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STORE_DATA_FAILED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STORE_DATA_RECEIVED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STORE_DATA_REQUESTED
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel(assistedFactory = LoginViewModel.Factory::class)
class LoginViewModel @AssistedInject constructor(
    private val fetchSiteData: FetchSiteData,
    private val analyticsTracker: AnalyticsTracker,
    @Assisted private val navController: NavHostController,
    savedState: SavedStateHandle
) : WearViewModel() {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init { requestSiteData() }

    override fun reloadData(withLoading: Boolean) { requestSiteData() }

    private fun requestSiteData() {
        analyticsTracker.track(WATCH_STORE_DATA_REQUESTED)
        _viewState.update { it.copy(isLoading = true) }
        launch {
            fetchSiteData().collect { loginState ->
                when (loginState) {
                    Logged -> {
                        analyticsTracker.track(WATCH_STORE_DATA_RECEIVED)
                        navController.navigate(MY_STORE.route) {
                            popUpTo(NavRoutes.LOGIN.route) { inclusive = true }
                        }
                    }
                    Timeout -> {
                        analyticsTracker.track(WATCH_STORE_DATA_FAILED)
                        _viewState.update { it.copy(isLoading = false) }
                    }
                    Waiting -> _viewState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = true
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): LoginViewModel
    }
}
