package com.woocommerce.android.ui.common

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.model.User
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class UserEligibilityErrorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val accountRepository: AccountRepository,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedState) {
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun start() {
        val email = appPrefs.getUserEmail()
        if (email.isNotEmpty()) {
            val user = userEligibilityFetcher.getUserByEmail(email)
            viewState = viewState.copy(user = user?.toAppModel())
        }
    }

    fun onLogoutButtonClicked() = launch {
        accountRepository.logout().let {
            if (it) {
                triggerEvent(Logout)
            }
        }
    }

    fun onRetryButtonClicked() {
        launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            userEligibilityFetcher.fetchUserInfo().fold(
                onSuccess = {
                    val isUserEligible = it.isUserEligible()

                    if (isUserEligible) {
                        triggerEvent(Exit)
                    } else triggerEvent(ShowSnackbar(string.user_role_access_error_retry))
                },
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.error_generic))
                }
            )

            viewState = viewState.copy(isProgressDialogShown = false)
        }
    }

    @Parcelize
    data class ViewState(
        val user: User? = null,
        val isProgressDialogShown: Boolean? = null
    ) : Parcelable
}
