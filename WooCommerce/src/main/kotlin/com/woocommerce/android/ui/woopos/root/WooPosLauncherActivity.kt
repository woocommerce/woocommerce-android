package com.woocommerce.android.ui.woopos.root

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.roles.lock.WooPosRestorePOS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WooPosLauncherActivity : ComponentActivity() {
    @Inject lateinit var restorePOS: WooPosRestorePOS

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { true }
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val destination = if (restorePOS()) WooPosActivity::class.java else MainActivity::class.java
            startActivity(Intent(this@WooPosLauncherActivity, destination).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            })
            finish()
        }
    }
}
