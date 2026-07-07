package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.CommentQuestion
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun WooPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = WooTheme.colors.surface.default,
            contentColor = WooTheme.colors.background.onSection,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PAGE_HEADER_HEIGHT)
                    .padding(horizontal = WooTheme.padding.padding7),
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    color = WooTheme.colors.background.onSection,
                    style = WooTheme.text.headlineSmall.strong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .height(PAGE_HEADER_ACTION_HEIGHT)
                        .widthIn(max = PAGE_HEADER_ACTION_MAX_WIDTH),
                    horizontalArrangement = Arrangement.spacedBy(
                        WooTheme.spacing.space1,
                        Alignment.End,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
        if (showDivider) {
            WooDivider()
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooPageHeaderPreview() {
    WooDesignSystemTheme {
        WooPageHeaderDemo()
    }
}

@Composable
internal fun WooPageHeaderDemo(
    modifier: Modifier = Modifier,
) {
    WooPageHeader(
        title = "Products",
        modifier = modifier,
        actions = {
            WooOutlinedIconButton(
                imageVector = WooIcons.Regular.ArrowUpRight,
                contentDescription = "Open",
                onClick = {},
            )
            WooIconButton(
                imageVector = WooIcons.Regular.CommentQuestion,
                contentDescription = "Help",
                onClick = {},
                emphasis = WooIconButtonEmphasis.Primary,
            )
        },
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooPageHeaderLongTitlePreview() {
    WooDesignSystemTheme {
        WooPageHeader(
            title = "A very long page header title that should truncate before the trailing actions",
            actions = {
                WooOutlinedIconButton(
                    imageVector = WooIcons.Regular.ArrowUpRight,
                    contentDescription = "Open",
                    onClick = {},
                )
                WooIconButton(
                    imageVector = WooIcons.Regular.CommentQuestion,
                    contentDescription = "Help",
                    onClick = {},
                    emphasis = WooIconButtonEmphasis.Primary,
                )
            },
        )
    }
}

private val PAGE_HEADER_HEIGHT = 64.dp
private val PAGE_HEADER_ACTION_HEIGHT = 40.dp
private val PAGE_HEADER_ACTION_MAX_WIDTH = 136.dp
