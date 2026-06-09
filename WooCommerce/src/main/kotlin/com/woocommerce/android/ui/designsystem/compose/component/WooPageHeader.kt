package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PAGE_HEADER_HEIGHT),
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = WooTheme.padding.padding7)
                        .semantics { heading() },
                    color = WooTheme.colors.background.onSection,
                    style = WooTheme.text.headlineSmall.strong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = WooTheme.padding.padding7)
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
        WooPageHeader(
            title = "Products",
            actions = {
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = "Open",
                    onClick = {},
                )
                WooIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_help_24dp),
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
