package com.woocommerce.android.ui.login.accountmismatch

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

@HiltViewModel
class AccountMismatchErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: AccountMismatchErrorFragmentArgs by savedStateHandle.navArgs()
    val viewState = flow {
        val userInfo = accountStore.account

        emit(
            ViewState(
                siteUrl = appPrefsWrapper.getLoginSiteAddress()!!,
                avatarUrl = userInfo?.avatarUrl.orEmpty(),
                username = userInfo?.userName.orEmpty(),
                displayName = userInfo?.displayName.orEmpty(),
                primaryButtonText = if (navArgs.hasConnectedStores) {
                    R.string.login_view_connected_stores
                } else {
                    R.string.login_site_picker_try_another_address
                },
                primaryButtonAction = {
                    if (navArgs.hasConnectedStores) {
                        showConnectedStores()
                    } else {
                        navigateToSiteAddressScreen()
                    }
                },
                secondaryButtonText = R.string.login_try_another_account,
                secondaryButtonAction = { logout() },
                inlineButtonText = R.string.login_need_help_finding_email,
                inlineButtonAction = { helpFindingEmail() }
            )
        )
    }.asLiveData()

    private fun showConnectedStores() {
        appPrefsWrapper.removeLoginSiteAddress()
        triggerEvent(Exit)
    }

    private fun navigateToSiteAddressScreen() {
        triggerEvent(NavigateToSiteAddressEvent)
    }

    private fun logout() {
        TODO("Not yet implemented")
    }

    private fun helpFindingEmail() {
        TODO("Not yet implemented")
    }

    fun onHelpButtonClick() {
        triggerEvent(NavigateToHelpScreen)
    }

    data class ViewState(
        val siteUrl: String,
        val avatarUrl: String,
        val displayName: String,
        val username: String,
        @StringRes val primaryButtonText: Int,
        val primaryButtonAction: () -> Unit,
        @StringRes val secondaryButtonText: Int,
        val secondaryButtonAction: () -> Unit,
        @StringRes val inlineButtonText: Int,
        val inlineButtonAction: () -> Unit
    )

    object NavigateToHelpScreen : MultiLiveEvent.Event()
    object NavigateToSiteAddressEvent : MultiLiveEvent.Event()
}
