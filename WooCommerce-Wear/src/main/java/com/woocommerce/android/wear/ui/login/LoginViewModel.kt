package com.woocommerce.android.wear.ui.login

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.extensions.getStateFlow
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Logged
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Timeout
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Waiting
import com.woocommerce.android.wear.viewmodel.WearViewModel
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STORE_DATA_FAILED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STORE_DATA_REQUESTED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_STORE_DATA_SUCCEEDED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val fetchSiteData: FetchSiteData,
    private val analyticsTracker: AnalyticsTracker,
    savedState: SavedStateHandle
) : WearViewModel() {
    @Suppress("ForbiddenComment")
    // TODO: Storing complete ViewState into SavedState can lead to TransactionTooLarge crashes. Only data that can't
    //  be easily recovered, such as user input, should be stored.
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init { requestSiteData() }

    override fun reloadData(withLoading: Boolean) { requestSiteData() }

    private fun requestSiteData() {
        analyticsTracker.track(WATCH_STORE_DATA_REQUESTED)
        launch {
            fetchSiteData().collect { loginState ->
                _viewState.update { it.copy(loginState = loginState) }
                when (loginState) {
                    Logged -> analyticsTracker.track(WATCH_STORE_DATA_SUCCEEDED)
                    Timeout -> analyticsTracker.track(WATCH_STORE_DATA_FAILED)
                    else -> { /* Do nothing */ }
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val loginState: LoginState = Waiting,
    ) : Parcelable

    enum class LoginState {
        Logged,
        Waiting,
        Timeout
    }
}
