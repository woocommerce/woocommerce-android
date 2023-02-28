package com.woocommerce.android.ui.login.jetpack.wpcom

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.GravatarUtils
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMagicLinkRequestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: JetpackActivationMagicLinkRequestFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState.MagicLinkRequestState(
            email = navArgs.emailOrUsername,
            avatarUrl = avatarUrlFromEmail(navArgs.emailOrUsername),
            isLoadingDialogShown = false
        )
    )
    val viewState = _viewState.asLiveData()

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    private fun avatarUrlFromEmail(email: String): String {
        val avatarSize = resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100)
        return GravatarUtils.gravatarFromEmail(email, avatarSize, GravatarUtils.DefaultImage.STATUS_404)
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        data class MagicLinkRequestState(
            val email: String,
            val avatarUrl: String,
            val isLoadingDialogShown: Boolean
        ) : ViewState

        @Parcelize
        data class MagicLinkSentState(
            val email: String
        ) : ViewState
    }
}
