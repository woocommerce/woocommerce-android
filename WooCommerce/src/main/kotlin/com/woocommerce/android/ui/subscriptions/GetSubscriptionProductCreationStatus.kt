package com.woocommerce.android.ui.subscriptions

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

/**
 * Woo Subscriptions 9.0 hides the Simple and Variable Subscription types behind two store settings.
 * Older plugin versions don't report them, so a missing value keeps the type creatable.
 */
class GetSubscriptionProductCreationStatus @Inject constructor(
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend operator fun invoke(): SubscriptionProductCreationStatus {
        if (!isEligibleForSubscriptions()) {
            return SubscriptionProductCreationStatus(
                isSimpleSubscriptionCreatable = false,
                isVariableSubscriptionCreatable = false
            )
        }

        val settings = wooCommerceStore.getSubscriptionProductCreationSettings(selectedSite.get())
            ?: wooCommerceStore.fetchSubscriptionProductCreationSettings(selectedSite.get())
                .takeUnless { it.isError }
                ?.model
        return SubscriptionProductCreationStatus(
            isSimpleSubscriptionCreatable = settings?.isSimpleSubscriptionCreationEnabled ?: true,
            isVariableSubscriptionCreatable = settings?.isVariableSubscriptionCreationEnabled ?: true
        )
    }

    data class SubscriptionProductCreationStatus(
        val isSimpleSubscriptionCreatable: Boolean,
        val isVariableSubscriptionCreatable: Boolean
    )
}
