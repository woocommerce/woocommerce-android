package com.woocommerce.android.ui.aisupportchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityAiSupportChatBinding
import com.woocommerce.android.extensions.doOnApplyWindowInsets
import com.woocommerce.android.extensions.parcelableArrayList
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiSupportChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAiSupportChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAiSupportChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.doOnApplyWindowInsets(consumeInsets = true) {
            binding.root.updatePadding(left = it.left, right = it.right, bottom = it.bottom)
            binding.appBarLayout.updatePadding(top = it.top)
        }

        setSupportActionBar(binding.toolbar.toolbar as Toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.ai_support_chat_screen_title)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, AiSupportChatActivity::class.java)

        fun createResumeIntent(
            context: Context,
            chatId: Long,
            botSlug: String,
            sessionId: String?
        ): Intent = Intent(context, AiSupportChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_BOT_SLUG, botSlug)
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        fun createConnectivityToolIntent(
            context: Context,
            checks: List<ConnectivityCheckCardData>
        ): Intent = Intent(context, AiSupportChatActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_CONNECTIVITY_CHECKS, ArrayList(checks))
        }

        fun launchModeFrom(intent: Intent): AiSupportChatLaunchMode {
            val extras = intent.extras ?: return AiSupportChatLaunchMode.Help
            val checks = extras.parcelableArrayList<ConnectivityCheckCardData>(EXTRA_CONNECTIVITY_CHECKS)
            if (!checks.isNullOrEmpty()) {
                return AiSupportChatLaunchMode.ConnectivityTool(checks)
            }

            if (extras.containsKey(EXTRA_CHAT_ID)) {
                return AiSupportChatLaunchMode.Resume(
                    chatId = extras.getLong(EXTRA_CHAT_ID),
                    botSlug = extras.getString(EXTRA_BOT_SLUG) ?: AiSupportChatViewModel.DEFAULT_BOT_SLUG,
                    sessionId = extras.getString(EXTRA_SESSION_ID)
                )
            }

            return AiSupportChatLaunchMode.Help
        }

        private const val EXTRA_CONNECTIVITY_CHECKS = "extra_connectivity_checks"
        private const val EXTRA_CHAT_ID = "extra_chat_id"
        private const val EXTRA_BOT_SLUG = "extra_bot_slug"
        private const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
