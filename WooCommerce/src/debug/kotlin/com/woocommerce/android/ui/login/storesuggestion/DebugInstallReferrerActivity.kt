package com.woocommerce.android.ui.login.storesuggestion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Lets adb fake the Play Install Referrer on debug builds, which can't receive one outside Google Play:
 *
 * ```
 * adb shell am start -n <package>/com.woocommerce.android.ui.login.storesuggestion.DebugInstallReferrerActivity \
 *   --es referrer "utm_campaign=deeplinks_promote_app&site_host=example.com" --el click_age_seconds 0
 * ```
 */
@AndroidEntryPoint
class DebugInstallReferrerActivity : AppCompatActivity() {
    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val clickAgeSeconds = intent.getLongExtra(EXTRA_CLICK_AGE_SECONDS, 0L)
            appPrefsWrapper.debugInstallReferrerOverride = intent.getStringExtra(EXTRA_REFERRER).orEmpty()
            appPrefsWrapper.debugInstallReferrerClickTimestampSeconds =
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - clickAgeSeconds
            appPrefsWrapper.isInstallReferrerChecked = false
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        finish()
    }

    private companion object {
        const val EXTRA_REFERRER = "referrer"
        const val EXTRA_CLICK_AGE_SECONDS = "click_age_seconds"
    }
}
