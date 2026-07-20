package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
            scrollBehavior.RenderMediumTopAppBar(
                title = {
                    PageHeaderTitle(
                        title = title,
                        style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = { PageHeaderActions(actions) },
                colors = colors,
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
                .height(PAGE_HEADER_ACTION_HEIGHT),
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

@Composable
internal fun WooPageHeaderInteractiveDemo(
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = WooPageHeaderDefaults.exitUntilCollapsedScrollBehavior()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        color = WooTheme.colors.background.section,
        shape = RoundedCornerShape(WooTheme.radius.medium),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WooPageHeader(
                title = "Store overview",
                scrollBehavior = scrollBehavior,
                actions = {
                    WooOutlinedIconButton(
                        imageVector = WooIcons.Regular.Share,
                        contentDescription = "Share store",
                        onClick = {},
                    )
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                repeat(PAGE_HEADER_DEMO_ITEM_COUNT) { index ->
                    WooCell(
                        title = "Store update ${index + 1}",
                        description = "Representative content in the collapsible page body.",
                    )
                }
            }
        }
    }
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
        val materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = state,
            snapAnimationSpec = null,
        )
        val scrollBehavior = remember(materialScrollBehavior) {
            WooPageHeaderScrollBehavior(materialScrollBehavior)
        }
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

private const val PAGE_HEADER_DEMO_ITEM_COUNT = 8
private val PAGE_HEADER_HEIGHT = 64.dp
private val PAGE_HEADER_ACTION_HEIGHT = 40.dp
