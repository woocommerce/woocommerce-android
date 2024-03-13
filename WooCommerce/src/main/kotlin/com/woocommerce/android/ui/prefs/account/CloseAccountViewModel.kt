package com.woocommerce.android.ui.prefs.account

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.CLOSE_ACCOUNT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CLOSE_ACCOUNT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CLOSE_ACCOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.AccountRepository.CloseAccountResult.Error
import com.woocommerce.android.ui.login.AccountRepository.CloseAccountResult.Success
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloseAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData(
        CloseAccountState(
            title = R.string.settings_close_account_dialog_title,
            description = R.string.settings_close_account_dialog_description,
            mainButtonText = R.string.settings_close_account_dialog_confirm_button,
            currentUserName = accountRepository.getUserAccount()?.userName
                ?: error("Account deletion setting requires user to log in with WP.com account"),
            enteredUserName = "",
            isLoading = false,
            isAccountDeletionError = false,
        )
    )
    val viewState = _viewState

    init {
        analyticsTrackerWrapper.track(CLOSE_ACCOUNT_TAPPED)
    }

    fun onConfirmCloseAccount() {
        launch {
            _viewState.value = _viewState.value?.copy(isLoading = true)
            when (val result = accountRepository.closeAccount()) {
                Success -> {
                    analyticsTrackerWrapper.track(CLOSE_ACCOUNT_SUCCESS)
                    triggerEvent(OnAccountClosed)
                }

                is Error -> {
                    val errorDescription =
                        if (result.hasActiveStores) {
                            R.string.settings_close_account_active_stores_error_description
                        } else {
                            R.string.settings_close_account_generic_error_description
                        }
                    _viewState.value = _viewState.value?.copy(
                        title = R.string.settings_close_account_error_dialog_title,
                        description = errorDescription,
                        mainButtonText = R.string.settings_close_account_dialog_contact_support_button,
                        isLoading = false,
                        isAccountDeletionError = true
                    )
                    analyticsTrackerWrapper.track(CLOSE_ACCOUNT_FAILED)
                }
            }
        }
    }

    fun onContactSupportClicked() {
        triggerEvent(ContactSupport(HelpOrigin.ACCOUNT_DELETION))
    }

    fun onCloseAccountDismissed() {
        triggerEvent(Exit)
    }

    fun onUserNameInputChanged(input: String) {
        _viewState.value = _viewState.value?.copy(enteredUserName = input)
    }

    data class CloseAccountState(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @StringRes val mainButtonText: Int,
        val currentUserName: String,
        val enteredUserName: String,
        val isAccountDeletionError: Boolean,
        val isLoading: Boolean
    )

    data class ContactSupport(val origin: HelpOrigin) : MultiLiveEvent.Event()
    object OnAccountClosed : MultiLiveEvent.Event()
}
