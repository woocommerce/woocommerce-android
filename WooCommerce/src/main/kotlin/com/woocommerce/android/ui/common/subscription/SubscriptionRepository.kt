package com.woocommerce.android.ui.common.subscription

import com.woocommerce.android.model.Subscription
import com.woocommerce.android.model.SubscriptionMapper
import com.woocommerce.android.network.subscription.SubscriptionRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

class SubscriptionRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val subscriptionRestClient: SubscriptionRestClient,
    private val subscriptionMapper: SubscriptionMapper,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun fetchSubscriptionsByOrderId(
        orderId: Long,
        site: SiteModel = selectedSite.get()
    ): WooResult<List<Subscription>> {
        return withContext(dispatchers.io) {
            val response = subscriptionRestClient.fetchSubscriptionsByOrderId(site, orderId)
            when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val subscriptions = response.result!!.map { dto -> subscriptionMapper.toAppModel(dto) }
                    WooResult(subscriptions)
                }
                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }

    suspend fun fetchSubscriptionsById(
        subscriptionId: Long,
        site: SiteModel = selectedSite.get()
    ): WooResult<Subscription> {
        return withContext(dispatchers.io) {
            val response = subscriptionRestClient.fetchSubscriptionsById(site, subscriptionId)
            when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val subscription = subscriptionMapper.toAppModel(response.result!!)
                    WooResult(subscription)
                }
                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }

    suspend fun createSubscription(
        site: SiteModel = selectedSite.get(),
        orderId: Long
    ): WooResult<Subscription> {
        return withContext(dispatchers.io) {
            val response = subscriptionRestClient.createSubscription(site, orderId)
            when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val subscription = subscriptionMapper.toAppModel(response.result!!)
                    WooResult(subscription)
                }
                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }
}
