package com.woocommerce.android.ui.payments

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.PreferredPluginResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.toInPersonPaymentsPluginType
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import javax.inject.Inject

/**
 * Use case to get the active payments plugin.
 */
class GetActivePaymentsPlugin @Inject constructor(
    private val prefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
) {
    suspend operator fun invoke(): WCInPersonPaymentsStore.InPersonPaymentsPluginType? =
        prefs.getCardReaderPreferredPlugin(
            selectedSite.get().id,
            selectedSite.get().siteId,
            selectedSite.get().selfHostedSiteId
        )?.toInPersonPaymentsPluginType() ?: fetchPreferredPlugin()

    private suspend fun fetchPreferredPlugin() =
        when (val preferredPluginResult = cardReaderOnboardingChecker.fetchPreferredPlugin()) {
            is PreferredPluginResult.Success -> preferredPluginResult.type.toInPersonPaymentsPluginType()
            else -> null
        }
}
