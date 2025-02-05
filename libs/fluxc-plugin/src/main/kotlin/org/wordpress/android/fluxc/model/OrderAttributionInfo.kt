package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.model.metadata.WCMetaData

data class OrderAttributionInfo(
    val sourceType: String? = null,
    val campaign: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val deviceType: String? = null,
    val sessionPageViews: String? = null
) {
    constructor(metadata: List<WCMetaData>) : this(
        sourceType = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.SOURCE_TYPE }?.valueAsString,
        campaign = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.CAMPAIGN }?.valueAsString,
        source = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.SOURCE }?.valueAsString,
        medium = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.MEDIUM }?.valueAsString,
        deviceType = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.DEVICE_TYPE }?.valueAsString,
        sessionPageViews = metadata.find {
            it.key == WCMetaData.OrderAttributionInfoKeys.SESSION_PAGE_VIEWS
        }?.valueAsString
    )
}
