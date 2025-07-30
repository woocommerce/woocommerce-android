package com.woocommerce.android.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParams
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParamsState
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeM3WithBackground
import com.woocommerce.android.util.RollingLogEntries
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.launch
import java.lang.String.format
import java.util.Locale

@Composable
fun WooLogViewerScreen(
    entries: RollingLogEntries,
    onBackPress: () -> Unit,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by rememberSaveable { mutableIntStateOf(0) }
    var previousSearchQuery by rememberSaveable { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val allEntries = remember(entries) { entries.toList() }

    val searchMatches = remember(allEntries, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allEntries.mapIndexedNotNull { index, entry ->
                if (entry.toString().contains(searchQuery, ignoreCase = true)) {
                    index
                } else {
                    null
                }
            }
        }
    }

    val hasMatches = searchMatches.isNotEmpty()
    val totalMatches = searchMatches.size

    LaunchedEffect(searchQuery) {
        if (searchQuery != previousSearchQuery) {
            previousSearchQuery = searchQuery
            if (searchQuery.isNotBlank()) {
                currentMatchIndex = 0
            }
        }
    }

    LaunchedEffect(searchMatches, currentMatchIndex) {
        if (hasMatches && currentMatchIndex < totalMatches) {
            val itemIndex = searchMatches[currentMatchIndex]
            coroutineScope.launch {
                lazyListState.animateScrollToItem(itemIndex)
            }
        }
    }

    val goToNextMatch = {
        if (hasMatches && currentMatchIndex < totalMatches - 1) {
            currentMatchIndex++
        }
    }

    val goToPreviousMatch = {
        if (hasMatches && currentMatchIndex > 0) {
            currentMatchIndex--
        }
    }
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.logviewer_activity_title),
                onNavigationButtonClick = onBackPress,
                actions = {
                    SearchNavigationActions(
                        hasMatches = hasMatches,
                        currentMatchIndex = currentMatchIndex,
                        totalMatches = totalMatches,
                        onPreviousClick = goToPreviousMatch,
                        onNextClick = goToNextMatch
                    )
                    IconButton(onClick = { onCopyButtonClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy_white_24dp),
                            contentDescription = stringResource(id = R.string.copy),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onShareButtonClick() }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(id = R.string.share),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchLayoutWithParams(
                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
                state = SearchLayoutWithParamsState(
                    hint = R.string.search,
                    searchQuery = searchQuery,
                    isSearchFocused = false,
                    areSearchTypesAlwaysVisible = false,
                    supportedSearchTypes = emptyList()
                ),
                paramsFillWidth = false,
                onSearchQueryChanged = { searchQuery = it },
                onSearchTypeSelected = { }
            )
            LogViewerEntries(
                entries = allEntries,
                lazyListState = lazyListState,
                currentMatchIndex = if (hasMatches) searchMatches[currentMatchIndex] else -1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchNavigationActions(
    hasMatches: Boolean,
    currentMatchIndex: Int,
    totalMatches: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    if (hasMatches) {
        Text(
            text = "${currentMatchIndex + 1}/$totalMatches",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.minor_100))
        )
        IconButton(
            onClick = onPreviousClick,
            enabled = currentMatchIndex > 0
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "",
                tint = if (currentMatchIndex > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
        IconButton(
            onClick = onNextClick,
            enabled = currentMatchIndex < totalMatches - 1
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "",
                tint = if (currentMatchIndex < totalMatches - 1) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Composable
private fun LogViewerEntries(
    entries: List<RollingLogEntries.LogEntry>,
    lazyListState: LazyListState,
    currentMatchIndex: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        itemsIndexed(entries) { index, entry ->
            LogViewerEntry(
                index = index,
                entry = entry,
                isCurrentMatch = index == currentMatchIndex
            )
            if (index < entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun LogViewerEntry(
    index: Int,
    entry: RollingLogEntries.LogEntry,
    isCurrentMatch: Boolean
) {
    val backgroundColor = if (isCurrentMatch) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor),
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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.minor_100)),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionContainer {
                Text(
                    text = entry.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = logLevelColorM3(entry.level)
                )
            }
        }
    }
}

@Composable
private fun logLevelColorM3(level: WooLog.LogLevel): Color {
    return when (level) {
        WooLog.LogLevel.v -> MaterialTheme.colorScheme.onSurfaceVariant
        WooLog.LogLevel.d -> MaterialTheme.colorScheme.tertiary
        WooLog.LogLevel.i -> MaterialTheme.colorScheme.primary
        WooLog.LogLevel.w -> Color(0xFFFF9800)
        WooLog.LogLevel.e -> MaterialTheme.colorScheme.error
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
    WooThemeM3WithBackground {
        WooLogViewerScreen(
            entries,
            onBackPress = {},
            onShareButtonClick = {},
            onCopyButtonClick = {}
        )
    }
}
