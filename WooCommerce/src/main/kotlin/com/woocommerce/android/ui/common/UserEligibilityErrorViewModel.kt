package com.woocommerce.android.ui.common

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.User
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class UserEligibilityErrorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedState) {
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    final fun start() {
        val email = appPrefs.getUserEmail()
        if (email.isNotEmpty()) {
            val user = userEligibilityFetcher.getUserByEmail(email)
            viewState = viewState.copy(user = user?.toAppModel())
        }
    }

    fun onLogoutButtonClicked() {
        // TODO: will be implemented in another commit
    }

    fun onRetryButtonClicked() {
        launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            val userModel = userEligibilityFetcher.fetchUserInfo()
            userModel?.let {
                val isUserEligible = it.isUserEligible()
                appPrefs.setIsUserEligible(isUserEligible)

                if (isUserEligible) {
                    triggerEvent(Exit)
                } else triggerEvent(ShowSnackbar(string.user_role_access_error_retry))
            }

            viewState = viewState.copy(isProgressDialogShown = false)
        }
    }

    fun onLearnMoreButtonClicked() {
        // TODO: will be implemented in another commit
    }

    @Parcelize
    data class ViewState(
        val user: User? = null,
        val isProgressDialogShown: Boolean? = null
    ) : Parcelable
}
