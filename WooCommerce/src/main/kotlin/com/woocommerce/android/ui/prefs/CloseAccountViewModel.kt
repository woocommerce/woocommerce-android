package com.woocommerce.android.ui.prefs

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloseAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
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

    fun onConfirmCloseAccount() {
        launch {
            _viewState.value = _viewState.value?.copy(isLoading = true)
            @Suppress("MagicNumber") delay(3000)
            _viewState.value = _viewState.value?.copy(
                title = R.string.settings_close_account_error_dialog_title,
                description = R.string.settings_close_account_error_dialog_description,
                mainButtonText = R.string.settings_close_account_dialog_contact_support_button,
                isLoading = false,
                isAccountDeletionError = true
            )
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
}
