package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.subscription.SubscriptionRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class GetOrderSubscriptions @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val subscriptionRepository: SubscriptionRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(orderId: Long): Result<List<Subscription>> {
        return withContext(dispatchers.io) {
            val renewalId = getRenewalId(orderId)
            if (renewalId != null) {
                getRenewalSubscription(renewalId)
            } else {
                getSubscriptions(orderId)
            }
        }
    }

    private suspend fun getRenewalId(orderId: Long): Long? {
        val metadataList = orderStore.getOrderMetadata(orderId, selectedSite.get())
        val renewal = metadataList.find { metadata ->
            metadata.key == WCMetaData.SubscriptionMetadataKeys.SUBSCRIPTION_RENEWAL
        }
        return renewal?.value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asLong
    }

    private suspend fun getRenewalSubscription(subscriptionId: Long): Result<List<Subscription>> {
        val result = subscriptionRepository.fetchSubscriptionsById(
            site = selectedSite.get(),
            subscriptionId = subscriptionId
        )
        return when {
            result.isError -> Result.failure(WooException(result.error))
            result.model != null -> Result.success(listOf(result.model!!))
            else -> Result.failure(Exception("Error fetching renewal subscription"))
        }
    }

    private suspend fun getSubscriptions(orderId: Long): Result<List<Subscription>> {
        val result = subscriptionRepository.fetchSubscriptionsByOrderId(
            site = selectedSite.get(),
            orderId = orderId
        )
        return when {
            result.isError -> Result.failure(WooException(result.error))
            result.model != null -> Result.success(result.model!!)
            else -> Result.failure(Exception("Error fetching subscriptions"))
        }
    }
}
