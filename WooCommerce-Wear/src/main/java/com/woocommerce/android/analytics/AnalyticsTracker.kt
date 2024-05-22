package com.woocommerce.android.analytics

import com.google.android.gms.wearable.DataMap
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.commons.DataPath
import javax.inject.Inject

class AnalyticsTracker @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository
) {
    fun track(stat: AnalyticsParameter, properties: Map<String, *> = emptyMap<String, Any>()) {

    }
}

enum class AnalyticsParameter {
    APP_OPENED,
    APP_CLOSED
}
