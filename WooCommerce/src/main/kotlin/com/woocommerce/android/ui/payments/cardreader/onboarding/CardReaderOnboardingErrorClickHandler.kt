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
    suspend operator fun invoke(errorType: CardReaderOnboardingCTAErrorType): Result =
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
                is PluginRepository.PluginStatus.PluginActivated -> Result.SUCCESS
                else -> Result.ERROR
            }
        }.last()

    enum class Result {
        SUCCESS, ERROR,
    }

    companion object {
        const val WC_PAY_SLUG = "woocommerce-payments"
        const val WC_PAY_NAME = "woocommerce-payments/woocommerce-payments"
    }
}

enum class CardReaderOnboardingCTAErrorType {
    WC_PAY_NOT_INSTALLED
}
