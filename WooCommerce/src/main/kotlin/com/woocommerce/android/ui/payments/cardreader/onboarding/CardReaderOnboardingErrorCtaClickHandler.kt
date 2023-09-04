package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WooCommerceStore
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
                cardReaderTracker.trackOnboardingCtaTapped(OnboardingCtaTapped.PLUGIN_INSTALL_TAPPED)

                installAndActivateWcPayPlugin().also {
                    it.errorMessage?.let { errorMessage ->
                        cardReaderTracker.trackOnboardingCtaFailed(
                            reason = OnboardingCtaTapped.PLUGIN_INSTALL_TAPPED,
                            description = errorMessage
                        )
                    }
                }
            }

            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_ACTIVATED -> {
                installAndActivateWcPayPlugin().also {
                    it.errorMessage?.let { errorMessage ->
                        cardReaderTracker.trackOnboardingCtaFailed(
                            reason = OnboardingCtaTapped.PLUGIN_ACTIVATE_TAPPED,
                            description = errorMessage
                        )
                    }
                }
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

                is PluginRepository.PluginStatus.PluginActivationFailed, ->
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
    }

    private val Reaction.errorMessage
        get() = when (this) {
            is Reaction.ShowErrorAndRefresh -> message
            else -> null
        }

    companion object {
        private const val WC_PAY_SLUG = "woocommerce-payments"
    }
}

enum class OnboardingCtaTapped(val value: String) {
    PLUGIN_INSTALL_TAPPED("plugin_install_tapped"),
    PLUGIN_ACTIVATE_TAPPED("plugin_activate_tapped"),
    CASH_ON_DELIVERY_TAPPED("cash_on_delivery_disabled"),
}

enum class CardReaderOnboardingCTAErrorType {
    WC_PAY_NOT_INSTALLED,
    WC_PAY_NOT_ACTIVATED,
}
