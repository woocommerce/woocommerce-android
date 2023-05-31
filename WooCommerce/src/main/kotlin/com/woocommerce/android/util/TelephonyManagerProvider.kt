package com.woocommerce.android.util

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TelephonyManagerProvider @Inject constructor(@ApplicationContext context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun getCountryCode(): String = telephonyManager.networkCountryIso
}
