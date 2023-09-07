package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.R
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

class CardReaderOnboardingErrorCtaClickHandler @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pluginRepository: PluginRepository,
    private val cardReaderTracker: CardReaderTracker,
    private val resourceProvider: ResourceProvider,
) {
    suspend operator fun invoke(errorType: CardReaderOnboardingCTAErrorType): Reaction =
        when (errorType) {
            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED -> {
                cardReaderTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.PLUGIN_INSTALL_TAPPED)

                installAndActivateWcPayPlugin().also {
                    it.errorMessage?.let { errorMessage ->
                        cardReaderTracker.trackOnboardingCtaFailed(
                            reason = OnboardingCtaReasonTapped.PLUGIN_INSTALL_TAPPED,
                            description = errorMessage
                        )
                    }
                }
            }

            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED -> {
                cardReaderTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.STRIPE_ACCOUNT_SETUP_TAPPED)

                installAndActivateWcPayPlugin().also {
                    it.errorMessage?.let { errorMessage ->
                        cardReaderTracker.trackOnboardingCtaFailed(
                            reason = OnboardingCtaReasonTapped.PLUGIN_ACTIVATE_TAPPED,
                            description = errorMessage
                        )
                    }
                }
            }

            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_SETUP -> {
                cardReaderTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.PLUGIN_SETUP_TAPPED)
                Reaction.OpenWebView(selectedSite.get().adminUrlOrDefault.slashJoin(WC_PAY_FINISH_SETUP_URL))
            }

            CardReaderOnboardingCTAErrorType.STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS -> {
                cardReaderTracker.trackOnboardingCtaTapped(OnboardingCtaReasonTapped.STRIPE_ACCOUNT_SETUP_TAPPED)
                Reaction.OpenWebView(selectedSite.get().adminUrlOrDefault.slashJoin(STRIPE_OVERDUE_REQUIREMENTS_URL))
            }
        }

    private suspend fun installAndActivateWcPayPlugin() =
        pluginRepository.installPlugin(
            site = selectedSite.get(),
            slug = WC_PAY_SLUG,
            name = WooCommerceStore.WooPlugin.WOO_PAYMENTS.pluginName,
        ).map { pluginStatus ->
            when (pluginStatus) {
                is PluginRepository.PluginStatus.PluginActivated,
                is PluginRepository.PluginStatus.PluginInstalled ->
                    Reaction.Refresh

                is PluginRepository.PluginStatus.PluginActivationFailed ->
                    buildShowErrorAndRefresh(pluginStatus.errorDescription)

                is PluginRepository.PluginStatus.PluginInstallFailed ->
                    buildShowErrorAndRefresh(pluginStatus.errorDescription)
            }
        }.last()

    private fun buildShowErrorAndRefresh(errorDescription: String) =
        Reaction.ShowErrorAndRefresh(
            message = errorDescription.ifEmpty {
                resourceProvider.getString(
                    R.string.error_generic
                )
            }
        )

    sealed class Reaction {
        object Refresh : Reaction()
        data class ShowErrorAndRefresh(val message: String) : Reaction()
        data class OpenWebView(val url: String) : Reaction()
    }

    private val Reaction.errorMessage
        get() = when (this) {
            is Reaction.ShowErrorAndRefresh -> message
            else -> null
        }

    companion object {
        private const val WC_PAY_SLUG = "woocommerce-payments"

        private const val WC_PAY_FINISH_SETUP_URL = "/wp-admin/admin.php?page=wc-admin&task=woocommerce-payments"
        private const val STRIPE_OVERDUE_REQUIREMENTS_URL =
            "/wp-admin/admin.php?page=wc-admin&task=woocommerce-payments"
    }
}

enum class OnboardingCtaReasonTapped(val value: String) {
    PLUGIN_INSTALL_TAPPED("plugin_install_tapped"),
    PLUGIN_ACTIVATE_TAPPED("plugin_activate_tapped"),
    PLUGIN_SETUP_TAPPED("plugin_setup_tapped"),
    STRIPE_ACCOUNT_SETUP_TAPPED("stripe_account_setup_tapped"),
    CASH_ON_DELIVERY_TAPPED("cash_on_delivery_disabled"),
}

enum class CardReaderOnboardingCTAErrorType {
    WC_PAY_NOT_INSTALLED,
    WC_PAY_NOT_ACTIVATED,
    WC_PAY_NOT_SETUP,
    STRIPE_ACCOUNT_OVERDUE_REQUIREMENTS,
}
