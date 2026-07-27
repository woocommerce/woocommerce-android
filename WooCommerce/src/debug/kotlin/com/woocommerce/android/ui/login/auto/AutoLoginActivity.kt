package com.woocommerce.android.ui.login.auto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutoLoginActivity : AppCompatActivity() {
    @Inject
    internal lateinit var requestStore: AutoLoginRequestStore

    @Inject
    internal lateinit var requestHandler: AutoLoginRequestHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent()
        } else {
            finish()
        }
    }

    private fun handleIntent() {
        lifecycleScope.launch {
            val status = runLogin()
            try {
                requestStore.publish(status)
                if (status.shouldNavigate) {
                    startActivity(
                        Intent(this@AutoLoginActivity, MainActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    )
                }
            } finally {
                finish()
            }
        }
    }

    private suspend fun runLogin(): AutoLoginStatus {
        if (!isValidInvocation()) {
            requestStore.consume()
            return AutoLoginStatus.INVALID_REQUEST
        }
        return try {
            when (val parsed = requestStore.consume()) {
                AutoLoginRequestParseResult.Invalid -> AutoLoginStatus.INVALID_REQUEST
                is AutoLoginRequestParseResult.Success -> requestHandler.login(parsed.request).toStatus()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            AutoLoginStatus.INTERNAL_ERROR
        }
    }

    private fun isValidInvocation(): Boolean {
        if (intent.action != ACTION_AUTO_LOGIN) return false
        if (intent.data != null) return false
        if (intent.clipData != null) return false
        return intent.extras == null
    }

    private fun AutoLoginResult.toStatus(): AutoLoginStatus = when (this) {
        AutoLoginResult.Success -> AutoLoginStatus.SUCCESS
        AutoLoginResult.AlreadyActive -> AutoLoginStatus.ALREADY_ACTIVE
        is AutoLoginResult.Failure -> status
    }

    companion object {
        const val ACTION_AUTO_LOGIN = "com.woocommerce.android.debug.AUTO_LOGIN"
    }
}
