package com.woocommerce.android.ui.aisupportchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
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
    private var contactSupportMenuItem: MenuItem? = null
    private var isContactSupportActionVisible = false
    var onContactSupportClicked: (() -> Unit)? = null

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_ai_support_chat, menu)
        contactSupportMenuItem = menu.findItem(R.id.menu_contact_support)
        contactSupportMenuItem?.isVisible = isContactSupportActionVisible
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.menu_contact_support -> {
                onContactSupportClicked?.invoke()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setContactSupportActionVisible(isVisible: Boolean) {
        isContactSupportActionVisible = isVisible
        contactSupportMenuItem?.isVisible = isVisible
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, AiSupportChatActivity::class.java)

        fun createPreLoginIntent(context: Context): Intent =
            Intent(context, AiSupportChatActivity::class.java).apply {
                putExtra(EXTRA_PRE_LOGIN, true)
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
            if (extras.getBoolean(EXTRA_PRE_LOGIN, false)) {
                return AiSupportChatLaunchMode.PreLogin
            }

            return AiSupportChatLaunchMode.Help
        }

        private const val EXTRA_CONNECTIVITY_CHECKS = "extra_connectivity_checks"
        private const val EXTRA_PRE_LOGIN = "extra_pre_login"
    }
}
