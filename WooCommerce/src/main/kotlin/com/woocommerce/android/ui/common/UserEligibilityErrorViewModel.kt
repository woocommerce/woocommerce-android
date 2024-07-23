package com.woocommerce.android.ui.common

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.User
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
    private val accountRepository: AccountRepository,
    private val userEligibilityFetcher: UserEligibilityFetcher,
    analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    companion object {
        private const val ROLES_KEY = "current_roles"
    }

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        userEligibilityFetcher.getUser()?.let { user ->
            viewState = viewState.copy(user = user)
            analyticsTracker.track(
                AnalyticsEvent.LOGIN_INSUFFICIENT_ROLE,
                mapOf(
                    ROLES_KEY to user.roles.joinToString(",") { it.value }
                )
            )
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
                    val isUserEligible = it.isEligible

                    if (isUserEligible) {
                        triggerEvent(Exit)
                    } else {
                        triggerEvent(ShowSnackbar(string.user_role_access_error_retry))
                    }
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
