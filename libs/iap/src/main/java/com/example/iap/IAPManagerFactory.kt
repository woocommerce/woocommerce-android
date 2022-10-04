package com.example.iap

import android.content.Context
import com.example.iap.internal.IAPManagerIml

object IAPManagerFactory {
    fun createIAPManager(
        context: Context,
        logWrapper: LogWrapper
    ): IAPManager = IAPManagerIml(context, logWrapper)
}
