package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CardReaderOnboardingErrorClickHandler @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pluginRepository: PluginRepository,
) {
    suspend operator fun invoke(errorType: CardReaderOnboardingCTAErrorType): Reaction =
        when (errorType) {
            CardReaderOnboardingCTAErrorType.WC_PAY_NOT_INSTALLED -> {
                installPlugin(
                    slug = WC_PAY_SLUG,
                    name = WC_PAY_NAME,
                )
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
                    Reaction.ShowErrorAndRefresh(message = pluginStatus.errorDescription)

                is PluginRepository.PluginStatus.PluginInstallFailed ->
                    Reaction.ShowErrorAndRefresh(message = pluginStatus.errorDescription)
            }
        }.last()

    sealed class Reaction {
        object Refresh : Reaction()
        data class ShowErrorAndRefresh(val message: String) : Reaction()
    }

    companion object {
        const val WC_PAY_SLUG = "woocommerce-payments"
        const val WC_PAY_NAME = "woocommerce-payments/woocommerce-payments"
    }
}

enum class CardReaderOnboardingCTAErrorType {
    WC_PAY_NOT_INSTALLED
}
