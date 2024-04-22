package com.woocommerce.android.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.wearable.Wearable
import com.woocommerce.android.sync.ConnectWearableUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectWearable: ConnectWearableUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent { WooWearNavHost() }
    }

    override fun onResume() {
        super.onResume()
        val dataClient = Wearable.getDataClient(this)
        connectWearable(
            dataClient,
            Wearable.getMessageClient(this),
            Wearable.getCapabilityClient(this)
        )

        dataClient.addListener {
            it.forEach {
                Log.d("DATA LAYER", "Data item received: $it")
            }
        }
    }
}
