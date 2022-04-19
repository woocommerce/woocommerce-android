package com.woocommerce.android.support

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ScreenLogviewerBinding
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.copyToClipboard
import org.wordpress.android.util.ToastUtils
import java.lang.String.format
import java.util.*

class WooLogViewerScreen : AppCompatActivity() {
    companion object {
        private const val ID_SHARE = 1
        private const val ID_COPY_TO_CLIPBOARD = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ScreenLogviewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar.toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val entries = WooLog.toHtmlList(AppThemeUtils.isDarkThemeActive(this@WooLogViewerScreen))

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    LogViewerEntries(entries)
                }
            }
        }
    }

    @Composable
    fun LogViewerEntries(entries: List<String>) {
        LazyColumn {
            itemsIndexed(entries) { index, entry ->
                LogViewerEntry(index, entry)
                if (index < entries.lastIndex)
                    Divider(
                        color = colorResource(id = R.color.divider_color),
                        thickness = 1.dp
                    )
            }
        }
    }

    @Composable
    fun LogViewerEntry(index: Int, item: String) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = format(Locale.US, "%02d", index + 1),
                style = MaterialTheme.typography.body2,
            )
            Text(
                text = HtmlCompat.fromHtml(item, FROM_HTML_MODE_LEGACY).toString(),
                style = MaterialTheme.typography.body2
            )
        }
    }

    private fun shareAppLog() {
        WooLog.addDeviceInfoEntry(T.DEVICE, WooLog.LogLevel.w)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, WooLog.toString())
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (ex: android.content.ActivityNotFoundException) {
            ToastUtils.showToast(this, R.string.logviewer_share_error)
        }
    }

    private fun copyAppLogToClipboard() {
        try {
            WooLog.addDeviceInfoEntry(T.DEVICE, WooLog.LogLevel.w)
            copyToClipboard("AppLog", WooLog.toString())
            ToastUtils.showToast(this, R.string.logviewer_copied_to_clipboard)
        } catch (e: Exception) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.logviewer_error_copy_to_clipboard)
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
                shareAppLog()
                true
            }
            ID_COPY_TO_CLIPBOARD -> {
                copyAppLogToClipboard()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
