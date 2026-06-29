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
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun createIntent(
            context: Context,
            preLogin: Boolean = false,
            siteAddress: String? = null
        ): Intent =
            Intent(context, AiSupportChatActivity::class.java).apply {
                putExtra(EXTRA_PRE_LOGIN, preLogin)
                putSiteAddressExtra(siteAddress)
            }

        fun createResumeIntent(
            context: Context,
            chatId: Long,
            botSlug: String,
            sessionId: String?,
            hasCreatedTicket: Boolean = false,
            isResolved: Boolean = false,
            extraTags: List<String> = emptyList(),
            siteAddress: String? = null
        ): Intent = Intent(context, AiSupportChatActivity::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_BOT_SLUG, botSlug)
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_HAS_CREATED_TICKET, hasCreatedTicket)
            putExtra(EXTRA_IS_RESOLVED, isResolved)
            putStringArrayListExtra(EXTRA_EXTRA_TAGS, ArrayList(extraTags))
            putSiteAddressExtra(siteAddress)
        }

        fun createConnectivityToolIntent(
            context: Context,
            checks: List<ConnectivityCheckCardData>,
            siteAddress: String? = null
        ): Intent = Intent(context, AiSupportChatActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_CONNECTIVITY_CHECKS, ArrayList(checks))
            putSiteAddressExtra(siteAddress)
        }

        fun createStoreConnectionErrorIntent(
            context: Context,
            siteAddress: String? = null
        ): Intent = Intent(context, AiSupportChatActivity::class.java).apply {
            putExtra(EXTRA_STORE_CONNECTION_ERROR, true)
            putSiteAddressExtra(siteAddress)
        }

        fun launchModeFrom(intent: Intent): AiSupportChatLaunchMode {
            val extras = intent.extras ?: return AiSupportChatLaunchMode.Help()
            val checks = extras.parcelableArrayList<ConnectivityCheckCardData>(EXTRA_CONNECTIVITY_CHECKS)
            val siteAddress = extras.getString(EXTRA_SITE_ADDRESS)?.takeIf { it.isNotBlank() }
            return when {
                !checks.isNullOrEmpty() -> AiSupportChatLaunchMode.ConnectivityTool(checks, siteAddress)
                extras.getBoolean(EXTRA_STORE_CONNECTION_ERROR, false) ->
                    AiSupportChatLaunchMode.StoreConnectionError(siteAddress)
                extras.getBoolean(EXTRA_PRE_LOGIN, false) -> AiSupportChatLaunchMode.PreLogin(siteAddress)
                extras.containsKey(EXTRA_CHAT_ID) -> AiSupportChatLaunchMode.Resume(
                    chatId = extras.getLong(EXTRA_CHAT_ID),
                    botSlug = extras.getString(EXTRA_BOT_SLUG) ?: AiSupportChatViewModel.DEFAULT_BOT_SLUG,
                    sessionId = extras.getString(EXTRA_SESSION_ID),
                    hasCreatedTicket = extras.getBoolean(EXTRA_HAS_CREATED_TICKET, false),
                    isResolved = extras.getBoolean(EXTRA_IS_RESOLVED, false),
                    extraTags = extras.getStringArrayList(EXTRA_EXTRA_TAGS).orEmpty(),
                    siteAddress = siteAddress
                )
                else -> AiSupportChatLaunchMode.Help(siteAddress)
            }
        }

        private fun Intent.putSiteAddressExtra(siteAddress: String?) {
            siteAddress?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_SITE_ADDRESS, it) }
        }

        private const val EXTRA_CONNECTIVITY_CHECKS = "extra_connectivity_checks"
        private const val EXTRA_STORE_CONNECTION_ERROR = "extra_store_connection_error"
        private const val EXTRA_PRE_LOGIN = "extra_pre_login"
        private const val EXTRA_CHAT_ID = "extra_chat_id"
        private const val EXTRA_BOT_SLUG = "extra_bot_slug"
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_HAS_CREATED_TICKET = "extra_has_created_ticket"
        private const val EXTRA_IS_RESOLVED = "extra_is_resolved"
        private const val EXTRA_EXTRA_TAGS = "extra_extra_tags"
        private const val EXTRA_SITE_ADDRESS = "extra_site_address"
    }
}
