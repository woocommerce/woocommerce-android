package com.woocommerce.android.ui.shipping

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.InstallWcShippingFlowEvent.ExitInstallFlowEvent
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.Step.Installation
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.Step.Onboarding
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.Step.PostInstallationFailure
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.Step.PostInstallationSuccess
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.Step.PreInstallation
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class InstallWcShippingFlowViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private companion object {
        const val WC_SHIPPING_INFO_URL = "https://woocommerce.com/woocommerce-shipping/"
    }

    private val step = savedState.getStateFlow<Step>(this, Step.Onboarding)

    val viewState = step
        .map { prepareStep(it) }
        .asLiveData()

    private fun prepareStep(step: Step): ViewState {
        return when (step) {
            Onboarding -> ViewState.Onboarding(
                title = string.install_wc_shipping_flow_onboarding_screen_title,
                subtitle = string.install_wc_shipping_flow_onboarding_screen_subtitle,
                bullets = getBulletPointsForInstallingWcShippingFlow(),
                linkUrl = WC_SHIPPING_INFO_URL,
                onLinkClicked = ::onLinkClicked,
                onInstallClicked = ::onInstallWcShippingClicked,
                onDismissFlowClicked = ::onDismissWcShippingFlowClicked
            )
            PreInstallation -> TODO()
            Installation -> TODO()
            PostInstallationSuccess -> TODO()
            is PostInstallationFailure -> TODO()
        }
    }

    private fun getBulletPointsForInstallingWcShippingFlow() =
        listOf(
            InstallWcShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_postage_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_postage_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_buy
            ),
            InstallWcShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_print_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_print_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_print
            ),
            InstallWcShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_discounts_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_discounts_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_disccounts
            ),
        )

    private fun onInstallWcShippingClicked() {
        step.value = Step.PreInstallation
    }

    private fun onDismissWcShippingFlowClicked() {
        triggerEvent(ExitInstallFlowEvent)
    }

    private fun onLinkClicked(url: String) {
        triggerEvent(InstallWcShippingFlowEvent.OpenLinkEvent(url))
    }

    sealed class InstallWcShippingFlowEvent : MultiLiveEvent.Event() {
        object ExitInstallFlowEvent : InstallWcShippingFlowEvent()
        data class OpenLinkEvent(
            val url: String,
        ) : InstallWcShippingFlowEvent()
    }

    private sealed interface Step : Parcelable {
        @Parcelize
        object Onboarding : Step

        @Parcelize
        object PreInstallation : Step

        @Parcelize
        object Installation : Step

        @Parcelize
        object PostInstallationSuccess : Step

        @Parcelize
        data class PostInstallationFailure(val errorMessage: String) : Step
    }

    sealed interface ViewState {
        data class Onboarding(
            @StringRes val title: Int,
            @StringRes val subtitle: Int,
            val bullets: List<InstallWcShippingOnboardingBulletUi>,
            val linkUrl: String,
            val onInstallClicked: () -> Unit = {},
            val onDismissFlowClicked: () -> Unit = {},
            val onLinkClicked: (String) -> Unit = {}
        ) : ViewState
    }

    data class InstallWcShippingOnboardingBulletUi(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @DrawableRes val icon: Int
    )

    object InstallWcShipping : MultiLiveEvent.Event()
}
