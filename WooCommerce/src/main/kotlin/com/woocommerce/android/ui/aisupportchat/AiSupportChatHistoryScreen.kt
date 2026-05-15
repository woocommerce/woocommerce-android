package com.woocommerce.android.ui.aisupportchat

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Composable
fun AiSupportChatHistoryScreen(
    viewModel: AiSupportChatHistoryViewModel,
    onBookmarkClicked: (SupportChatBookmark) -> Unit
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    AiSupportChatHistoryScreen(
        viewState = viewState,
        onBookmarkClicked = onBookmarkClicked
    )
}

@Composable
fun AiSupportChatHistoryScreen(
    viewState: AiSupportChatHistoryViewState,
    onBookmarkClicked: (SupportChatBookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        viewState.isLoading -> LoadingHistory(modifier = modifier)
        viewState.bookmarks.isEmpty() -> EmptyHistory(
            showError = viewState.showError,
            modifier = modifier
        )
        else -> HistoryList(
            bookmarks = viewState.bookmarks,
            onBookmarkClicked = onBookmarkClicked,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingHistory(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyHistory(
    showError: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100)),
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_comment),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(
                    if (showError) {
                        R.string.ai_support_chat_history_load_error
                    } else {
                        R.string.ai_support_chat_history_empty
                    }
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun HistoryList(
    bookmarks: List<SupportChatBookmark>,
    onBookmarkClicked: (SupportChatBookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(dimensionResource(R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.major_100))
    ) {
        items(bookmarks, key = { it.chatId }) { bookmark ->
            HistoryRow(
                bookmark = bookmark,
                onClick = { onBookmarkClicked(bookmark) }
            )
        }
    }
}

@Composable
private fun HistoryRow(
    bookmark: SupportChatBookmark,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.major_100),
                vertical = dimensionResource(R.dimen.major_100)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title ?: stringResource(R.string.ai_support_chat_history_default_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_50)))
                Text(
                    text = bookmark.updatedAt.relativeTime(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Long.relativeTime(): String =
    DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()

@LightDarkThemePreviews
@Composable
private fun AiSupportChatHistoryScreenPreview() {
    WooThemeWithBackground {
        AiSupportChatHistoryScreen(
            viewState = AiSupportChatHistoryViewState(
                bookmarks = listOf(
                    SupportChatBookmark(
                        chatId = 1L,
                        localSiteId = LocalId(1),
                        remoteSiteId = 2L,
                        botSlug = AiSupportChatViewModel.DEFAULT_BOT_SLUG,
                        sessionId = "session-id",
                        title = "My orders are fine, all is good",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            ),
            onBookmarkClicked = {}
        )
    }
}
