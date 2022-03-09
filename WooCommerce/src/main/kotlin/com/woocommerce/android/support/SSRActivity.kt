package com.woocommerce.android.support

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivitySsrBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.copyToClipboard
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import java.lang.IllegalStateException

@AndroidEntryPoint
class SSRActivity : AppCompatActivity() {
    companion object {
        private const val ID_SHARE = 1
        private const val ID_COPY_TO_CLIPBOARD = 2
    }

    private val viewModel: SSRActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySsrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar.toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupObservers(binding)
    }

    override fun onResume() {
        super.onResume()

        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_SSR_OPENED)
    }

    private fun setupObservers(binding: ActivitySsrBinding) {
        viewModel.viewStateData.observe(this) { old, new ->
            new.formattedSSR.takeIfNotEqualTo(old?.formattedSSR) {
                binding.ssrContent.text = it
            }

            new.isLoading.takeIfNotEqualTo(old?.isLoading) {
                if (it) {
                    binding.ssrLoading.show()
                    binding.ssrContent.hide()
                } else {
                    binding.ssrLoading.hide()
                    binding.ssrContent.show()
                }
            }
        }
        viewModel.event.observe(this) {
            when (it) {
                is ShareSSR -> shareSSR(it.ssrText)
                is CopySSR -> copySSRToClipboard(it.ssrText)
                is ShowSnackbar -> ToastUtils.showToast(this, it.message)
                is Exit -> finish()
                else -> it.isHandled = false
            }
        }
    }

    private fun shareSSR(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: android.content.ActivityNotFoundException) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.support_system_status_report_share_error)
        }
    }

    private fun copySSRToClipboard(text: String) {
        try {
            copyToClipboard(getString(R.string.support_system_status_report_clipboard_label), text)
            AnalyticsTracker.track(AnalyticsEvent.SUPPORT_SSR_COPY_BUTTON_TAPPED)
            ToastUtils.showToast(this, R.string.support_system_status_report_copied_to_clipboard)
        } catch (e: IllegalStateException) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.support_system_status_report_error_copy_to_clipboard)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val mnuCopy = menu.add(
            Menu.NONE,
            ID_COPY_TO_CLIPBOARD, Menu.NONE, android.R.string.copy
        )
        mnuCopy.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        mnuCopy.setIcon(R.drawable.ic_copy_white_24dp)

        val mnuShare = menu.add(
            Menu.NONE,
            ID_SHARE, Menu.NONE, R.string.share
        )
        mnuShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        mnuShare.setIcon(R.drawable.ic_share_white_24dp)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            ID_SHARE -> {
                viewModel.onShareButtonTapped()
                true
            }
            ID_COPY_TO_CLIPBOARD -> {
                viewModel.onCopyButtonTapped()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
