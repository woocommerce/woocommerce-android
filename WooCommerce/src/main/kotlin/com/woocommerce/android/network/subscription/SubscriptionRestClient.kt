package com.woocommerce.android.network.subscription

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class SubscriptionRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchSubscriptionsByOrderId(site: SiteModel, orderId: Long): WooPayload<Array<SubscriptionDto>> {
        val url = WOOCOMMERCE.subscriptions.pathV3
        val params = mapOf("parent" to "$orderId")

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<SubscriptionDto>::class.java,
            params = params
        ).toWooPayload()
    }

    suspend fun fetchSubscriptionsById(site: SiteModel, subscriptionId: Long): WooPayload<SubscriptionDto> {
        val url = WOOCOMMERCE.subscriptions.id(subscriptionId).pathV3

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = SubscriptionDto::class.java
        ).toWooPayload()
    }

    suspend fun createSubscription(site: SiteModel, orderId: Long): WooPayload<SubscriptionDto> {
        val url = WOOCOMMERCE.orders.id(orderId).subscriptions.pathV3
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = SubscriptionDto::class.java,
        ).toWooPayload()
    }

    @Suppress("PropertyName", "VariableNaming")
    class SubscriptionDto : Response {
        val id: Long? = null
        val status: String? = null
        val billing_period: String? = null
        val billing_interval: String? = null
        val start_date_gmt: String? = null
        val end_date_gmt: String? = null
        val total: String? = null
        val currency: String? = null
    }
}

