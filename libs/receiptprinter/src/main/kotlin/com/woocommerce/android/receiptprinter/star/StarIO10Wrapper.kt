package com.woocommerce.android.receiptprinter.star

import android.content.Context
import android.os.Build
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarDeviceDiscoveryManagerFactory
import com.starmicronics.stario10.StarIO10DiagInfoUpload
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class StarIO10Wrapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val androidSdkVersion: Int
        get() = Build.VERSION.SDK_INT

    fun disableDiagnosticsUpload() {
        StarIO10DiagInfoUpload.getInstance().isEnabled = false
    }

    fun createDiscoveryManager(interfaceTypes: List<InterfaceType>): StarDeviceDiscoveryManager =
        StarDeviceDiscoveryManagerFactory.create(interfaceTypes, context)
}
