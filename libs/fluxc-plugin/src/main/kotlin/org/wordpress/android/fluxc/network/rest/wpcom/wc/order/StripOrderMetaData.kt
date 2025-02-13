package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaData.OrderAttributionInfoKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaData.SubscriptionMetadataKeys
import javax.inject.Inject

class StripOrderMetaData @Inject internal constructor() {
    companion object {
        private val SUPPORTED_KEYS: Set<String> = buildSet {
            add(SubscriptionMetadataKeys.SUBSCRIPTION_RENEWAL)
            addAll(OrderAttributionInfoKeys.ALL_KEYS)
        }
    }

    operator fun invoke(metaData: List<WCMetaData>): List<WCMetaData> {
        return metaData
            .filter {
                it.isDisplayable || it.key in SUPPORTED_KEYS
            }
    }
}
