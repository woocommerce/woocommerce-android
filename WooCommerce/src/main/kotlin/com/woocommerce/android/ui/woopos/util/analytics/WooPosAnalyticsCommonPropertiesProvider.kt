package com.woocommerce.android.ui.woopos.util.analytics

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.DeviceType
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.EntryPoint
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
        get() = when (displaySmallestWidthDp >= TABLET_SMALLEST_WIDTH_DP) {
            true -> DeviceType.TABLET
            false -> DeviceType.PHONE
        }

    private val displaySmallestWidthDp: Int
        get() {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            val display = checkNotNull(displayManager.getDisplay(Display.DEFAULT_DISPLAY))
            return context.createDisplayContext(display).resources.configuration.smallestScreenWidthDp
        }

    private companion object {
        const val TABLET_SMALLEST_WIDTH_DP = 600
    }
}

@Singleton
class WooPosAnalyticsEntryPointKeeper @Inject constructor() {
    var entryPoint: EntryPoint? = null
        private set

    fun onPosEntered(entryPoint: EntryPoint) {
        this.entryPoint = entryPoint
    }

    fun onPosSessionEnded() {
        entryPoint = null
    }
}
