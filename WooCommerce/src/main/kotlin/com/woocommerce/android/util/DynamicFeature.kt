package com.woocommerce.android.util

import android.content.Context
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import javax.inject.Inject

class DynamicFeature @Inject constructor(private val appContext: Context) {
    fun installModule(moduleName: String) {
        appContext.applicationContext?.let { context ->
            val splitInstallManager = SplitInstallManagerFactory.create(context)
            val request = SplitInstallRequest.newBuilder()
                .addModule(moduleName)
                .build()

            splitInstallManager.startInstall(request)
                .addOnSuccessListener {
                    Log.d("WooCommerceApp", it.toString())
                }
                .addOnFailureListener {
                    Log.e("WooCommerceApp", it.toString())
                }
        }
    }
}
