package com.woocommerce.android.support

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParams
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParamsState
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.logs.LogEntry
import kotlinx.coroutines.launch
import java.lang.String.format
import java.util.Locale

@Composable
fun WooLogViewerScreen(
    viewerViewModel: WooLogViewerViewModel
) {
    viewerViewModel.uiState.observeAsState().value?.let { state ->
        // TODO animate transitions between states
        //  For now, I had to remove the AnimatedContent as it caused a very noticeable delay
        //  when navigating to the LogFileContent state when the log file is large (> 1000 entries).
        when (state) {
            is WooLogViewerViewModel.UiState.LogFilesList -> {
                LogFilesListScreen(state)
            }

            is WooLogViewerViewModel.UiState.LogFileContent -> {
                LogFileContent(state)
            }
        }
    }
}

@Composable
private fun LogFilesListScreen(state: WooLogViewerViewModel.UiState.LogFilesList) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.logviewer_activity_title),
                onNavigationButtonClick = { backDispatcher?.onBackPressedDispatcher?.onBackPressed() },
                actions = {
                    IconButton(onClick = state.onShareAllClicked) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_share_24dp),
                            contentDescription = stringResource(id = R.string.logviewer_share_all_logs),
                            tint = colorResource(id = R.color.color_icon_menu)
                        )
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.color_toolbar))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.logviewer_log_files_list_header),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            itemsIndexed(state.logFiles) { index, logFile ->
                if (index == 0) {
                    HorizontalDivider()
                }
                Text(
                    text = logFile.displayName.getText(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable(onClick = { state.onLogFileSelected(logFile) })
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp
                        ),
                )
                HorizontalDivider()
            }

            item {
                Text(
                    text = stringResource(R.string.logviewer_log_files_list_footer),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (state.isPreparingArchive) {
        ProgressDialog(
            title = "",
            subtitle = stringResource(id = R.string.logviewer_preparing_logs)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogFileContent(
    state: WooLogViewerViewModel.UiState.LogFileContent
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by rememberSaveable { mutableIntStateOf(0) }
    var previousSearchQuery by rememberSaveable { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val searchMatches = remember(state.logEntries, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            state.logEntries.mapIndexedNotNull { index, entry ->
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

    BackHandler(onBack = state.onBackPressed)

    Scaffold(
        topBar = {
            Toolbar(
                title = state.logFile.displayName.getText(),
                onNavigationButtonClick = state.onBackPressed,
                windowInsets = TopAppBarDefaults.windowInsets,
                actions = {
                    SearchNavigationActions(
                        hasMatches = hasMatches,
                        currentMatchIndex = currentMatchIndex,
                        totalMatches = totalMatches,
                        onPreviousClick = goToPreviousMatch,
                        onNextClick = goToNextMatch
                    )
                    if (searchQuery.isBlank()) {
                        IconButton(onClick = state.onCopyClicked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_copy_white_24dp),
                                contentDescription = stringResource(id = R.string.copy),
                                tint = colorResource(id = R.color.color_icon_menu)
                            )
                        }
                        IconButton(onClick = state.onShareClicked) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_share_24dp),
                                contentDescription = stringResource(id = R.string.share),
                                tint = colorResource(id = R.color.color_icon_menu)
                            )
                        }
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchLayoutWithParams(
                modifier = Modifier.background(color = colorResource(id = R.color.color_toolbar)),
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
                entries = state.logEntries,
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
                ImageVector.vectorResource(R.drawable.ic_arrow_up),
                contentDescription = "",
                tint = if (currentMatchIndex > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                }
            )
        }
        IconButton(
            onClick = onNextClick,
            enabled = currentMatchIndex < totalMatches - 1
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_arrow_down),
                contentDescription = "",
                tint = if (currentMatchIndex < totalMatches - 1) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Composable
private fun LogViewerEntries(
    entries: List<LogEntry>,
    lazyListState: LazyListState,
    currentMatchIndex: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
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
    entry: LogEntry,
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
private fun LogFilesListPreview() {
    val logFiles = listOf(
        WooLogViewerViewModel.LogFile("log1.txt", UiString.UiStringText("Log File 1")),
        WooLogViewerViewModel.LogFile("log2.txt", UiString.UiStringText("Log File 2")),
        WooLogViewerViewModel.LogFile("log3.txt", UiString.UiStringText("Log File 3"))
    )
    val state = WooLogViewerViewModel.UiState.LogFilesList(
        logFiles = logFiles,
        onLogFileSelected = {},
        onShareAllClicked = {}
    )
    WooTheme {
        LogFilesListScreen(state)
    }
}

@Preview
@Composable
private fun LogFileContentPreview() {
    val entries = buildList {
        add(LogEntry(WooLog.T.ORDERS, WooLog.LogLevel.v, "Verbose"))
        add(LogEntry(WooLog.T.PRODUCTS, WooLog.LogLevel.d, "Debug"))
        add(LogEntry(WooLog.T.REVIEWS, WooLog.LogLevel.i, "Informational"))
        add(LogEntry(WooLog.T.SUPPORT, WooLog.LogLevel.w, "Warning"))
        add(LogEntry(WooLog.T.DASHBOARD, WooLog.LogLevel.e, "Error"))
    }
    WooTheme {
        LogFileContent(
            state = WooLogViewerViewModel.UiState.LogFileContent(
                logFile = WooLogViewerViewModel.LogFile(
                    "log_example.txt",
                    UiString.UiStringText("Example Log File")
                ),
                logEntries = entries,
                onBackPressed = {},
                onShareClicked = {},
                onCopyClicked = {}
            )
        )
    }
}
