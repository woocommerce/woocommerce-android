package com.woocommerce.android.ui.common

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.model.User
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject

@HiltViewModel
class UserEligibilityErrorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedState) {
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    final fun start() {
        val email = appPrefs.getUserEmail()
        if (email.isNotEmpty()) {
            val user = userEligibilityFetcher.getUserByEmail(email)
            viewState = viewState.copy(user = user?.toAppModel())
        }
    }

    fun onLogoutButtonClicked() {
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
    }

    fun onRetryButtonClicked() {
        launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            val userModel = userEligibilityFetcher.fetchUserInfo()
            userModel?.let {
                val isUserEligible = it.isUserEligible()
                userEligibilityFetcher.updateUserInfo(it)

                if (isUserEligible) {
                    triggerEvent(Exit)
                } else triggerEvent(ShowSnackbar(string.user_role_access_error_retry))
            }

            viewState = viewState.copy(isProgressDialogShown = false)
        }
    }

    @Parcelize
    data class ViewState(
        val user: User? = null,
        val isProgressDialogShown: Boolean? = null
    ) : Parcelable

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            WooLog.e(
                T.LOGIN,
                "Account error [type = ${event.causeOfChange}] : " +
                    "${event.error.type} > ${event.error.message}"
            )
        } else if (!accountStore.hasAccessToken()) {
            triggerEvent(Logout)
        }
    }
}
