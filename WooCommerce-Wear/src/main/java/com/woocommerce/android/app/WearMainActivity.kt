package com.woocommerce.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.ui.WooWearNavHost
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_APP_OPENED
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent { WooWearNavHost() }
        analyticsTracker.track(WATCH_APP_OPENED)
    }
}
