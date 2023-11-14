package com.woocommerce.android.util

import android.content.Context
import android.nfc.NfcAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import javax.inject.Inject

class DeviceFeatures @Inject constructor(
    private val context: Context
) {
    fun isGooglePlayServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun isNFCAvailable(): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null
}
