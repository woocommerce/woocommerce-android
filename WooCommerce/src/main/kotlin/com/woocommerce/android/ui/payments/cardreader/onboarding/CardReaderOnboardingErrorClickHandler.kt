package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CardReaderOnboardingErrorClickHandler @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pluginRepository: PluginRepository,
    private val cardReaderTracker: CardReaderTracker,
    private val resourceProvider: ResourceProvider,
) {
    suspend operator fun invoke(errorType: CardReaderOnboardingCTAErrorType): Reaction =
        when (errorType) {
            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED -> {
                cardReaderTracker.trackOnboardingCtaTapped(OnboardingCtaTapped.PLUGIN_INSTALL_TAPPED)

                val pluginInstallationResult = installPlugin(slug = WC_PAY_SLUG, name = WC_PAY_NAME)

                if (pluginInstallationResult is Reaction.ShowErrorAndRefresh) {
                    cardReaderTracker.trackOnboardingCtaFailed(
                        reason = OnboardingCtaTapped.PLUGIN_INSTALL_TAPPED,
                        description = pluginInstallationResult.message
                    )
                }

                pluginInstallationResult
            }
        }

    private suspend fun installPlugin(slug: String, name: String) =
        pluginRepository.installPlugin(
            site = selectedSite.get(),
            slug = slug,
            name = name,
        ).map { pluginStatus ->
            when (pluginStatus) {
                is PluginRepository.PluginStatus.PluginActivated,
                is PluginRepository.PluginStatus.PluginInstalled ->
                    Reaction.Refresh

                is PluginRepository.PluginStatus.PluginActivationFailed ->
                    Reaction.ShowErrorAndRefresh(
                        message = pluginStatus.errorDescription.ifEmpty {
                            resourceProvider.getString(
                                R.string.error_generic
                            )
                        }
                    )

                is PluginRepository.PluginStatus.PluginInstallFailed ->
                    Reaction.ShowErrorAndRefresh(
                        message = pluginStatus.errorDescription.ifEmpty {
                            resourceProvider.getString(
                                R.string.error_generic
                            )
                        }
                    )
            }
        }.last()

    sealed class Reaction {
        object Refresh : Reaction()
        data class ShowErrorAndRefresh(val message: String) : Reaction()
    }

    companion object {
        private const val WC_PAY_SLUG = "woocommerce-payments"
        private const val WC_PAY_NAME = "woocommerce-payments/woocommerce-payments"
    }
}

enum class OnboardingCtaTapped(val value: String) {
    PLUGIN_INSTALL_TAPPED("plugin_install_tapped"),
    CASH_ON_DELIVERY_TAPPED("cash_on_delivery_disabled"),
}

enum class CardReaderOnboardingCTAErrorType {
    WC_PAY_NOT_INSTALLED
}
