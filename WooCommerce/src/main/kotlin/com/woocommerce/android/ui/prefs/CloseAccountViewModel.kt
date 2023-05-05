package com.woocommerce.android.ui.prefs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
                ?: error("Account deletion setting requires user to log in with WP.com account")
        )
    )
    val viewState = _viewState

    fun onCloseAccountClicked() {
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.store_onboarding_dialog_title,
                messageId = R.string.store_onboarding_dialog_description,
                positiveButtonId = R.string.remove,
                positiveBtnAction = { dialog, _ ->

                    dialog.dismiss()
                },
                negativeBtnAction = { dialog, _ -> dialog.dismiss() },
                negativeButtonId = R.string.cancel,
            )
        )
    }

    data class CloseAccountState(
        val userName: String,
        val isLoading: Boolean = false
    )
}
