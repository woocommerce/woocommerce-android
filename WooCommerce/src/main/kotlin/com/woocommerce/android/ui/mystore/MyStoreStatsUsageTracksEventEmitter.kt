package com.woocommerce.android.ui.mystore

import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.*
import javax.inject.Inject

@ActivityRetainedScoped
class MyStoreStatsUsageTracksEventEmitter @Inject constructor() {
    fun interacted(at: Date = Date()) {
        println("$this interacted at $at")
    }
}
