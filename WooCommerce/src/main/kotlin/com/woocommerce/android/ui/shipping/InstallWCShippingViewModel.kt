package com.woocommerce.android.ui.shipping

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.Step.Installation
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class InstallWCShippingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    private companion object {
        private const val WC_SHIPPING_INFO_URL = "https://woocommerce.com/woocommerce-shipping/"
    }

    private val step = savedState.getStateFlow<Step>(this, Step.Onboarding)

    val viewState = step
        .map { prepareStep(it) }
        .asLiveData()

    init {
        launch {
            // Wait for installation step
            step.filter { it == Installation }
                .first()

            // TODO launch plugin installation
        }
    }

    private fun prepareStep(step: Step): ViewState {
        return when (step) {
            Step.Onboarding -> ViewState.Onboarding(
                title = R.string.install_wc_shipping_flow_onboarding_screen_title,
                subtitle = R.string.install_wc_shipping_flow_onboarding_screen_subtitle,
                bullets = getBulletPointsForInstallingWcShippingFlow(),
                onInfoLinkClicked = { onLinkClicked(WC_SHIPPING_INFO_URL) },
                onInstallClicked = ::onInstallWcShippingClicked,
                onDismissFlowClicked = ::onDismissWcShippingFlowClicked
            )
            Step.PreInstallation -> ViewState.InstallationState.PreInstallation(
                extensionsName = R.string.install_wc_shipping_extension_name,
                siteName = selectedSite.get().let { site ->
                    site.displayName?.takeIf { it.isNotBlank() } ?: site.name.orEmpty()
                },
                siteUrl = selectedSite.get().url.orEmpty(),
                onCancelClick = ::onDismissWcShippingFlowClicked,
                onProceedClick = ::onStartInstallation,
                onInfoClick = { onLinkClicked("https://url") } // TODO
            )
            Step.Installation -> ViewState.InstallationState.InstallationOngoing(
                extensionsName = R.string.install_wc_shipping_extension_name,
                siteName = selectedSite.get().let { site ->
                    site.displayName?.takeIf { it.isNotBlank() } ?: site.name.orEmpty()
                },
                siteUrl = selectedSite.get().url.orEmpty()
            )
            Step.PostInstallationSuccess -> TODO()
            is Step.PostInstallationFailure -> TODO()
        }
    }

    private fun getBulletPointsForInstallingWcShippingFlow() =
        listOf(
            InstallWCShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_postage_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_postage_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_buy
            ),
            InstallWCShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_print_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_print_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_print
            ),
            InstallWCShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_discounts_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_discounts_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_disccounts
            ),
        )

    private fun onInstallWcShippingClicked() {
        step.value = Step.PreInstallation
    }

    private fun onDismissWcShippingFlowClicked() {
        triggerEvent(Exit)
    }

    private fun onLinkClicked(url: String) {
        triggerEvent(OpenLinkEvent(url))
    }

    private fun onStartInstallation() {
        step.value = Step.Installation
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
            val bullets: List<InstallWCShippingOnboardingBulletUi>,
            val onInstallClicked: () -> Unit = {},
            val onDismissFlowClicked: () -> Unit = {},
            val onInfoLinkClicked: () -> Unit = {}
        ) : ViewState

        sealed class InstallationState : ViewState {
            abstract val extensionsName: Int
            abstract val siteName: String
            abstract val siteUrl: String

            data class PreInstallation(
                @StringRes override val extensionsName: Int,
                override val siteName: String,
                override val siteUrl: String,
                val onCancelClick: () -> Unit,
                val onProceedClick: () -> Unit,
                val onInfoClick: () -> Unit
            ) : InstallationState()

            data class InstallationOngoing(
                @StringRes override val extensionsName: Int,
                override val siteName: String,
                override val siteUrl: String,
            ) : InstallationState()
        }
    }

    data class InstallWCShippingOnboardingBulletUi(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @DrawableRes val icon: Int
    )

    object InstallWcShipping : MultiLiveEvent.Event()
    data class OpenLinkEvent(
        val url: String,
    ) : MultiLiveEvent.Event()
}
