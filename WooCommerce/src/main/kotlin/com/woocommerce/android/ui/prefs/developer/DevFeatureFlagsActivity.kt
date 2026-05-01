package com.woocommerce.android.ui.prefs.developer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevFeatureFlagsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WooThemeWithBackground {
                DevFeatureFlagsScreen(
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onRestartClick = { restartApp() }
                )
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    companion object {
        private const val SKIP_REMOTE_LOAD_KEY = DevFeatureFlagsViewModel.SKIP_REMOTE_LOAD_KEY

        fun createIntent(context: Context, skipRemoteLoad: Boolean = false): Intent {
            return Intent(context, DevFeatureFlagsActivity::class.java).apply {
                putExtra(SKIP_REMOTE_LOAD_KEY, skipRemoteLoad)
            }
        }
    }
}
