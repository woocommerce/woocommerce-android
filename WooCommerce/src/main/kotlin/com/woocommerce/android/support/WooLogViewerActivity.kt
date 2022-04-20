package com.woocommerce.android.support

import android.content.Intent
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityLogviewerBinding
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.RollingLogEntries
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.copyToClipboard
import org.wordpress.android.util.ToastUtils
import java.lang.String.format
import java.util.*

class WooLogViewerActivity : AppCompatActivity() {
    private val isDarkThemeEnabled: Boolean by lazy {
        AppThemeUtils.isDarkThemeActive(this@WooLogViewerActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLogviewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setSupportActionBar(binding.toolbar.toolbar as Toolbar)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    WooLogViewerScreen()
                }
            }
        }
    }

    @Composable
    fun WooLogViewerScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    backgroundColor = MaterialTheme.colors.surface,
                    title = { Text(stringResource(id = R.string.logviewer_activity_title)) },
                    navigationIcon = {
                        IconButton(onClick = { onBackPressed() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { copyAppLogToClipboard() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_copy_white_24dp),
                                contentDescription = stringResource(id = R.string.copy),
                                tint = colorResource(id = R.color.color_icon_menu)
                            )
                        }
                        IconButton(onClick = { shareAppLog() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share_white_24dp),
                                contentDescription = stringResource(id = R.string.share),
                                tint = colorResource(id = R.color.color_icon_menu)
                            )
                        }
                    }
                )
            }
        ) {
            LogViewerEntries(WooLog.logEntries)
        }
    }

    @Composable
    fun LogViewerEntries(entries: RollingLogEntries) {
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
    fun LogViewerEntry(index: Int, entry: RollingLogEntries.LogEntry) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.surface),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.major_100),
                    vertical = dimensionResource(R.dimen.minor_100)
                ),
            ) {
                Text(
                    text = format(Locale.US, "%02d", index + 1),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(dimensionResource(R.dimen.minor_100)),
                    color = colorResource(id = R.color.grey)
                )
                SelectionContainer {
                    Text(
                        text = entry.toString(),
                        style = MaterialTheme.typography.body2,
                        color = colorResource(id = logLevelColor(entry.level))
                    )
                }
            }
        }
    }

    @ColorRes
    private fun logLevelColor(level: WooLog.LogLevel): Int {
        return if (isDarkThemeEnabled) {
            R.color.white
        } else {
            when (level) {
                WooLog.LogLevel.v -> R.color.grey
                WooLog.LogLevel.d -> R.color.blue_50
                WooLog.LogLevel.i -> R.color.woo_black
                WooLog.LogLevel.w -> R.color.woo_purple_50
                WooLog.LogLevel.e -> R.color.woo_red_50
            }
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
}
