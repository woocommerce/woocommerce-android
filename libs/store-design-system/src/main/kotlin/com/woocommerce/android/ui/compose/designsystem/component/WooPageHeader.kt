package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.CommentQuestion
import com.woocommerce.android.ui.compose.designsystem.icons.Share
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    scrollBehavior: WooPageHeaderScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val colors = pageHeaderColors()
        if (scrollBehavior == null) {
            TopAppBar(
                title = {
                    PageHeaderTitle(
                        title = title,
                        style = WooTheme.text.headlineSmall.strong,
                    )
                },
                actions = { PageHeaderActions(actions) },
                expandedHeight = PAGE_HEADER_HEIGHT,
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = colors,
            )
        } else {
            MediumTopAppBar(
                title = {
                    PageHeaderTitle(
                        title = title,
                        style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = { PageHeaderActions(actions) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = colors,
                scrollBehavior = scrollBehavior.delegate,
            )
        }
        if (showDivider) {
            WooDivider()
        }
    }
}

@Composable
private fun PageHeaderTitle(
    title: String,
    style: TextStyle,
) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WooTheme.padding.padding3,
                end = WooTheme.padding.padding6,
            )
            .semantics { heading() },
        color = WooTheme.colors.background.onSection,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PageHeaderActions(
    actions: @Composable RowScope.() -> Unit,
) {
    Box(modifier = Modifier.padding(end = WooTheme.padding.padding5)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun pageHeaderColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = WooTheme.colors.surface.default,
    scrolledContainerColor = WooTheme.colors.surface.default,
    titleContentColor = WooTheme.colors.background.onSection,
    actionIconContentColor = WooTheme.colors.primary,
)

/**
 * Defaults for [WooPageHeader]. Attach [WooPageHeaderScrollBehavior.nestedScrollConnection] to the caller's
 * scrolling content when using [exitUntilCollapsedScrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class)
object WooPageHeaderDefaults {
    @Composable
    fun exitUntilCollapsedScrollBehavior(
        canScroll: () -> Boolean = { true },
    ): WooPageHeaderScrollBehavior {
        val delegate = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = canScroll,
            snapAnimationSpec = null,
        )
        return remember(delegate) { WooPageHeaderScrollBehavior(delegate) }
    }
}

@Stable
@OptIn(ExperimentalMaterial3Api::class)
class WooPageHeaderScrollBehavior internal constructor(
    internal val delegate: TopAppBarScrollBehavior,
) {
    val nestedScrollConnection: NestedScrollConnection
        get() = delegate.nestedScrollConnection
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

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Preview(name = "Medium expanded")
@Composable
private fun WooPageHeaderMediumExpandedPreview() {
    WooPageHeaderMediumPreview(collapsedFraction = 0f)
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Preview(name = "Medium partial, long title at 2x font", fontScale = 2f)
@Composable
private fun WooPageHeaderMediumPartiallyCollapsedPreview() {
    WooPageHeaderMediumPreview(
        title = "A very long page header title that truncates before the share action",
        collapsedFraction = 0.5f,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Preview(name = "Medium collapsed RTL", locale = "ar")
@Composable
private fun WooPageHeaderMediumCollapsedPreview() {
    WooPageHeaderMediumPreview(
        title = "متجر WooCommerce طويل الاسم",
        collapsedFraction = 1f,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WooPageHeaderMediumPreview(
    collapsedFraction: Float,
    title: String = "Example Store",
) {
    WooDesignSystemTheme {
        val collapseRange = with(LocalDensity.current) {
            (TopAppBarDefaults.MediumAppBarCollapsedHeight - TopAppBarDefaults.MediumAppBarExpandedHeight).toPx()
        }
        val state = remember(collapsedFraction, collapseRange) {
            TopAppBarState(
                initialHeightOffsetLimit = collapseRange,
                initialHeightOffset = collapseRange * collapsedFraction,
                initialContentOffset = 0f,
            )
        }
        val delegate = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = state,
            snapAnimationSpec = null,
        )
        val scrollBehavior = remember(delegate) { WooPageHeaderScrollBehavior(delegate) }
        WooPageHeader(
            title = title,
            scrollBehavior = scrollBehavior,
            actions = {
                WooOutlinedIconButton(
                    imageVector = WooIcons.Regular.Share,
                    contentDescription = "Share",
                    onClick = {},
                )
            },
        )
    }
}

private val PAGE_HEADER_HEIGHT = 64.dp
private val PAGE_HEADER_ACTION_HEIGHT = 40.dp
private val PAGE_HEADER_ACTION_MAX_WIDTH = 136.dp
