package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.model.metadata.WCMetaData.AddOnsMetadataKeys
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import javax.inject.Inject

class StripProductMetaData @Inject internal constructor() {
    operator fun invoke(metaData: List<WCMetaData>): List<WCMetaData> {
        return metaData.filter { it.isDisplayable || SUPPORTED_KEYS.contains(it.key) }
    }

    companion object {
        val SUPPORTED_KEYS: Set<String> = buildSet {
            add(AddOnsMetadataKeys.ADDONS_METADATA_KEY)
            addAll(SubscriptionMetadataKeys.ALL_KEYS)
        }
    }
}
