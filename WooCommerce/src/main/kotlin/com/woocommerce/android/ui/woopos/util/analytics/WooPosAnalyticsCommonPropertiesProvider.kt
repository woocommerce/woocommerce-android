package com.woocommerce.android.ui.woopos.util.analytics

import android.content.Context
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.DeviceType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.EntryPoint
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout
import javax.inject.Inject
import javax.inject.Singleton

class WooPosAnalyticsCommonPropertiesProvider @Inject constructor(
    private val context: Context,
    private val entryPointKeeper: WooPosAnalyticsEntryPointKeeper,
) {
    val commonProperties: Map<String, String>
        get() = buildMap {
            put(DeviceType.DEVICE_TYPE, deviceType.value)
            entryPointKeeper.entryPoint?.let { put(EntryPoint.ENTRY_POINT, it.value) }
        }

    private val deviceType: DeviceType
        get() = when (context.isWooPosPhoneLayout()) {
            true -> DeviceType.PHONE
            false -> DeviceType.TABLET
        }
}

@Singleton
class WooPosAnalyticsEntryPointKeeper @Inject constructor() {
    @Volatile
    var entryPoint: EntryPoint? = null
        private set

    fun onPosEntered(entryPoint: EntryPoint) {
        this.entryPoint = entryPoint
    }

    fun onPosSessionEnded() {
        entryPoint = null
    }
}
