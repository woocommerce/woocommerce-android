package com.woocommerce.android.ui.woopos.util

import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Reusable
import javax.inject.Inject

@Reusable
class WooPosBluetoothStatus @Inject constructor(
    private val context: Context
) {
    fun isEnabled(): Boolean =
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
}
