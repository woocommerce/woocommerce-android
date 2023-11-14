package com.woocommerce.android.support

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.util.RollingLogEntries
import com.woocommerce.android.util.WooLog
import java.lang.String.format
import java.util.Locale

@Composable
fun WooLogViewerScreen(
    entries: RollingLogEntries,
    onBackPress: () -> Unit,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.logviewer_activity_title),
                onNavigationButtonClick = onBackPress,
                actions = {
                    IconButton(onClick = { onCopyButtonClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy_white_24dp),
                            contentDescription = stringResource(id = R.string.copy),
                            tint = colorResource(id = R.color.color_icon_menu)
                        )
                    }
                    IconButton(onClick = { onShareButtonClick() }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(id = R.string.share),
                            tint = colorResource(id = R.color.color_icon_menu)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LogViewerEntries(
            entries,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun LogViewerEntries(
    entries: RollingLogEntries,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(entries) { index, entry ->
            LogViewerEntry(index, entry)
            if (index < entries.lastIndex) {
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun LogViewerEntry(index: Int, entry: RollingLogEntries.LogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.surface),
        verticalArrangement = Arrangement.Top,
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
                modifier = Modifier.padding(end = dimensionResource(R.dimen.minor_100)),
                color = colorResource(id = R.color.woo_gray_40)
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
    return when (level) {
        WooLog.LogLevel.v -> R.color.log_text_verbose
        WooLog.LogLevel.d -> R.color.log_text_debug
        WooLog.LogLevel.i -> R.color.log_text_info
        WooLog.LogLevel.w -> R.color.log_text_warning
        WooLog.LogLevel.e -> R.color.log_text_error
    }
}

@Preview
@Composable
private fun WooLogViewerScreenPreview() {
    val entries = RollingLogEntries(99).also {
        it.add(
            RollingLogEntries.LogEntry(WooLog.T.ORDERS, WooLog.LogLevel.v, "Verbose")
        )
        it.add(
            RollingLogEntries.LogEntry(WooLog.T.PRODUCTS, WooLog.LogLevel.d, "Debug")
        )
        it.add(
            RollingLogEntries.LogEntry(WooLog.T.REVIEWS, WooLog.LogLevel.i, "Informational")
        )
        it.add(
            RollingLogEntries.LogEntry(WooLog.T.SUPPORT, WooLog.LogLevel.w, "Warning")
        )
        it.add(
            RollingLogEntries.LogEntry(WooLog.T.DASHBOARD, WooLog.LogLevel.e, "Error")
        )
    }
    WooLogViewerScreen(
        entries,
        onBackPress = {},
        onShareButtonClick = {},
        onCopyButtonClick = {}
    )
}
