package com.woocommerce.android.ui.woopos.lock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.root.WooPosActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resolve only a marked POS launch here, without restoring any management fragments or dialogs. */
@AndroidEntryPoint
internal class WooPosRestoreActivity : ComponentActivity() {
    @Inject
    lateinit var restoreLockedPOS: WooPosRestoreLockedPOS

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { true }
        super.onCreate(null)
        lifecycleScope.launch {
            val destination = if (restoreLockedPOS()) WooPosActivity::class.java else MainActivity::class.java
            startActivity(
                Intent(this@WooPosRestoreActivity, destination)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        }
    }
}
