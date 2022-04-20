package com.woocommerce.android.support

import androidx.annotation.ColorRes
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.util.RollingLogEntries
import com.woocommerce.android.util.WooLog
import java.lang.String.format
import java.util.*

private var isDarkTheme: Boolean = false

@Composable
fun WooLogViewerScreen(
    isDarkThemeEnabled: Boolean,
    onBackPress: () -> Unit,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit
) {
    isDarkTheme = isDarkThemeEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                title = { Text(stringResource(id = R.string.logviewer_activity_title)) },
                navigationIcon = {
                    IconButton(onClick = { onBackPress() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
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
    return if (isDarkTheme) {
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
