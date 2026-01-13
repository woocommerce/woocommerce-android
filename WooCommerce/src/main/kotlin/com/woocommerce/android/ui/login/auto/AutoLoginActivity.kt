package com.woocommerce.android.ui.login.auto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

@AndroidEntryPoint
class AutoLoginActivity : AppCompatActivity() {

    @Inject
    lateinit var autoLoginHandler: AutoLoginHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null || !autoLoginHandler.isAutoLoginUri(uri)) {
            finish()
            return
        }

        autoLoginHandler.handleAutoLogin(
            uri = uri,
            scope = lifecycleScope,
            onSuccess = {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                val loginIntent = Intent(this, LoginActivity::class.java)
                LoginMode.WOO_LOGIN_MODE.putInto(loginIntent)
                startActivity(loginIntent)
                finish()
            }
        )
    }
}
