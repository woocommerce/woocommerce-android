package com.woocommerce.android.ui.mystore

import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

class MyStoreStatsUsageTracksEventEmitter @Inject constructor() {
    fun interacted(at: Date = Date()) {
        println("$this interacted at $at")
    }
}
