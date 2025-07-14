package com.woocommerce.android.wear.analytics

import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.commons.DataParameters.ANALYTICS_PARAMETERS
import com.woocommerce.commons.DataParameters.ANALYTICS_TRACK
import com.woocommerce.commons.DataPath
import com.woocommerce.commons.WearAnalyticsEvent
import javax.inject.Inject

class AnalyticsTracker @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository
) {
    private val gson by lazy { Gson() }

    fun track(stat: WearAnalyticsEvent, properties: Map<String, String> = emptyMap()) {
        val parameters = gson.toJson(properties)
        phoneRepository.sendData(
            dataPath = DataPath.ANALYTICS_DATA,
            data = DataMap().apply {
                putString(ANALYTICS_TRACK.value, stat.name)
                putString(ANALYTICS_PARAMETERS.value, parameters)
            }
        )
    }
}
