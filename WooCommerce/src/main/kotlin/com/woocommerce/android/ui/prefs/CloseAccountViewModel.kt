package com.woocommerce.android.ui.prefs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CloseAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData(
        CloseAccountState(
            userName = accountRepository.getUserAccount()?.userName
                ?: error("Account deletion setting requires user to log in with WP.com account"),
            enteredUserNameError = null
        )
    )
    val viewState = _viewState

    fun onConfirmCloseAccount() {

    }

    fun onCloseAccountDismissed() {
        triggerEvent(Exit)
    }

    fun onUserNameInputChanged(input: String) {
        _viewState.value = _viewState.value?.copy(enteredUserName = input)
    }

    data class CloseAccountState(
        val userName: String,
        val enteredUserName: String = "",
        val enteredUserNameError: String?,
        val isLoading: Boolean = false
    )
}
