package com.woocommerce.android.ui.login.jetpack.wpcom

import android.os.Parcelable
import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.DefaultAvatarOption
import com.gravatar.types.Email
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_LOGIN_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.MagicLinkFlow
import com.woocommerce.android.ui.login.MagicLinkSource
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMagicLinkRequestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val wpComLoginRepository: WPComLoginRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: JetpackActivationMagicLinkRequestFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow<ViewState>(
        scope = viewModelScope,
        initialValue = ViewState.MagicLinkRequestState(
            emailOrUsername = navArgs.emailOrUsername,
            avatarUrl = avatarUrlFromEmail(navArgs.emailOrUsername),
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            allowPasswordLogin = !navArgs.isAccountPasswordless,
            isLoadingDialogShown = false
        )
    )
    val viewState = _viewState.asLiveData()

    init {
        requestMagicLink()
    }

    fun onRequestMagicLinkClick() = requestMagicLink()

    fun onOpenEmailClientClick() {
        triggerEvent(OpenEmailClient)
    }

    fun onUsePasswordClick() {
        triggerEvent(Exit)
    }

    fun onCloseClick() {
        triggerEvent(Exit)

        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_MAGIC_LINK,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_DISMISS
            )
        )
    }

    private fun requestMagicLink() = launch {
        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_MAGIC_LINK,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_SUBMIT
            )
        )

        _viewState.value = ViewState.MagicLinkRequestState(
            emailOrUsername = navArgs.emailOrUsername,
            avatarUrl = avatarUrlFromEmail(navArgs.emailOrUsername),
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            allowPasswordLogin = !navArgs.isAccountPasswordless,
            isLoadingDialogShown = true
        )
        val source = when {
            !navArgs.jetpackStatus.isJetpackInstalled -> MagicLinkSource.JetpackInstallation
            !navArgs.jetpackStatus.isJetpackConnected -> MagicLinkSource.JetpackConnection
            else -> MagicLinkSource.WPComAuthentication
        }
        wpComLoginRepository.requestMagicLink(
            emailOrUsername = navArgs.emailOrUsername,
            flow = MagicLinkFlow.SiteCredentialsToWPCom,
            source = source
        ).fold(
            onSuccess = {
                _viewState.value = ViewState.MagicLinkSentState(
                    email = navArgs.emailOrUsername.takeIf { it.isAnEmail() },
                    isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
                    allowPasswordLogin = !navArgs.isAccountPasswordless,
                )
            },
            onFailure = {
                _viewState.update { (it as ViewState.MagicLinkRequestState).copy(isLoadingDialogShown = false) }
                triggerEvent(ShowSnackbar(R.string.error_generic))

                analyticsTrackerWrapper.track(
                    JETPACK_SETUP_LOGIN_FLOW,
                    mapOf(
                        AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_MAGIC_LINK,
                        AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_SUBMIT
                    )
                )
            }
        )
    }

    private fun avatarUrlFromEmail(email: String): String {
        val avatarSize = resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100)
        return AvatarUrl(
            Email(email),
            AvatarQueryOptions(preferredSize = avatarSize, defaultAvatarOption = DefaultAvatarOption.Status404)
        ).toString()
    }

    private fun String.isAnEmail() = PatternsCompat.EMAIL_ADDRESS.matcher(this).matches()

    sealed interface ViewState : Parcelable {
        val isJetpackInstalled: Boolean
        val allowPasswordLogin: Boolean

        @Parcelize
        data class MagicLinkRequestState(
            val emailOrUsername: String,
            val avatarUrl: String,
            override val isJetpackInstalled: Boolean,
            override val allowPasswordLogin: Boolean,
            val isLoadingDialogShown: Boolean
        ) : ViewState

        @Parcelize
        data class MagicLinkSentState(
            val email: String?,
            override val isJetpackInstalled: Boolean,
            override val allowPasswordLogin: Boolean
        ) : ViewState
    }

    object OpenEmailClient : MultiLiveEvent.Event()
}
