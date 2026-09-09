package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.AngleLeft
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

/** Visual size of a [WooTopAppBar]. */
enum class WooTopAppBarSize {
    Small,
    Medium,
}

/** Horizontal alignment of the app-bar title content. */
enum class WooTopAppBarTitleAlignment {
    Start,
    Center,
}

/**
 * Native WooCommerce top app bar.
 *
 * Two mutually exclusive families are available:
 * - Fixed (this overload): the bar never collapses. Pass [showDivider] to control the bottom divider directly,
 *   e.g. from a scroll state's `canScrollBackward`.
 * - Collapsible: pass a required [WooTopAppBarScrollBehavior] and attach its `nestedScrollConnection` to the
 *   container that owns the scrolling content. The divider is derived automatically from how far content has
 *   scrolled under the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    size: WooTopAppBarSize = WooTopAppBarSize.Small,
    titleAlignment: WooTopAppBarTitleAlignment = WooTopAppBarTitleAlignment.Start,
    supportingText: String? = null,
    showDivider: Boolean = false,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {},
) {
    WooTopAppBarLayout(
        title = {
            TopAppBarTitle(
                title = title,
                supportingText = supportingText,
                size = size,
                titleAlignment = titleAlignment,
            )
        },
        modifier = modifier,
        navigationIcon = topAppBarNavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationClick,
        ),
        windowInsets = windowInsets,
        size = size,
        titleAlignment = titleAlignment,
        scrollBehavior = null,
        showDivider = showDivider,
        actions = { WooTopAppBarScopedActions(actions) },
    )
}

/** See the fixed-family [WooTopAppBar] overload for the divider contract. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    size: WooTopAppBarSize = WooTopAppBarSize.Small,
    titleAlignment: WooTopAppBarTitleAlignment = WooTopAppBarTitleAlignment.Start,
    showDivider: Boolean = false,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {},
) {
    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
        size = size,
        titleAlignment = titleAlignment,
        scrollBehavior = null,
        showDivider = showDivider,
        actions = { WooTopAppBarScopedActions(actions) },
    )
}

/**
 * Collapsible [WooTopAppBar]. Attach [scrollBehavior]'s `nestedScrollConnection` to the container that owns the
 * scrolling content; the divider is derived automatically from how far content has scrolled under the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    size: WooTopAppBarSize = WooTopAppBarSize.Small,
    titleAlignment: WooTopAppBarTitleAlignment = WooTopAppBarTitleAlignment.Start,
    supportingText: String? = null,
    scrollBehavior: WooTopAppBarScrollBehavior,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {},
) {
    WooTopAppBarLayout(
        title = {
            TopAppBarTitle(
                title = title,
                supportingText = supportingText,
                size = size,
                titleAlignment = titleAlignment,
            )
        },
        modifier = modifier,
        navigationIcon = topAppBarNavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationClick,
        ),
        windowInsets = windowInsets,
        size = size,
        titleAlignment = titleAlignment,
        scrollBehavior = scrollBehavior,
        showDivider = false,
        actions = { WooTopAppBarScopedActions(actions) },
    )
}

/** See the collapsible [WooTopAppBar] overload above for the divider contract. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    size: WooTopAppBarSize = WooTopAppBarSize.Small,
    titleAlignment: WooTopAppBarTitleAlignment = WooTopAppBarTitleAlignment.Start,
    scrollBehavior: WooTopAppBarScrollBehavior,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {},
) {
    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
        size = size,
        titleAlignment = titleAlignment,
        scrollBehavior = scrollBehavior,
        showDivider = false,
        actions = { WooTopAppBarScopedActions(actions) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WooTopAppBarLayout(
    title: @Composable () -> Unit,
    modifier: Modifier,
    navigationIcon: @Composable () -> Unit,
    windowInsets: WindowInsets,
    size: WooTopAppBarSize,
    titleAlignment: WooTopAppBarTitleAlignment,
    scrollBehavior: WooTopAppBarScrollBehavior?,
    showDivider: Boolean,
    actions: @Composable RowScope.() -> Unit,
) {
    val titleColor = WooTheme.colors.surface.onDefault
    val actionColors = wooTopAppBarActionColors(WooTheme.colors)
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = WooTheme.colors.surface.bright,
        scrolledContainerColor = WooTheme.colors.surface.bright,
        navigationIconContentColor = titleColor,
        titleContentColor = titleColor,
        actionIconContentColor = actionColors.contentColor,
    )
    val isScrolled by remember(scrollBehavior, showDivider) {
        derivedStateOf {
            showDivider || (scrollBehavior?.overlappedFraction ?: 0f) > SCROLLED_FRACTION_THRESHOLD
        }
    }
    val appBarTitle: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalContentColor provides titleColor,
        ) {
            ProvideTextStyle(LocalTextStyle.current.copy(fontWeight = WooTheme.text.titleLarge.emphasized.fontWeight)) {
                title()
            }
        }
    }
    Box(modifier = modifier) {
        when (size) {
            WooTopAppBarSize.Small -> when (titleAlignment) {
                WooTopAppBarTitleAlignment.Start -> TopAppBar(
                    title = appBarTitle,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = colors,
                    scrollBehavior = scrollBehavior?.materialScrollBehaviorDelegate,
                )

                WooTopAppBarTitleAlignment.Center -> CenterAlignedTopAppBar(
                    title = appBarTitle,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = colors,
                    scrollBehavior = scrollBehavior?.materialScrollBehaviorDelegate,
                )
            }

            WooTopAppBarSize.Medium -> MediumTopAppBar(
                title = appBarTitle,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior?.materialScrollBehaviorDelegate,
            )
        }
        if (isScrolled) {
            WooDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag(WOO_TOP_APP_BAR_DIVIDER_TEST_TAG),
            )
        }
    }
}

@Composable
private fun TopAppBarTitle(
    title: String,
    supportingText: String?,
    size: WooTopAppBarSize,
    titleAlignment: WooTopAppBarTitleAlignment,
) {
    val textAlign = if (titleAlignment == WooTopAppBarTitleAlignment.Center) TextAlign.Center else TextAlign.Start
    val titleModifier = if (titleAlignment == WooTopAppBarTitleAlignment.Center) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
    }
    Column(modifier = titleModifier.semantics { heading() }) {
        Text(
            text = title,
            color = WooTheme.colors.surface.onDefault,
            style = LocalTextStyle.current,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        supportingText?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                color = WooTheme.colors.surface.onVariant,
                style = supportingTextStyle(size),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun supportingTextStyle(size: WooTopAppBarSize) = when (size) {
    WooTopAppBarSize.Small -> WooTheme.text.bodySmall.regular.copy(fontSize = 12.sp, lineHeight = 16.sp)
    WooTopAppBarSize.Medium -> WooTheme.text.bodyMedium.regular.copy(fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun topAppBarNavigationIcon(
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String?,
    onNavigationClick: (() -> Unit)?,
): @Composable () -> Unit {
    if (navigationIcon == null) return {}

    val navigationClick = requireNotNull(onNavigationClick) {
        "WooTopAppBar requires onNavigationClick when navigationIcon is set"
    }
    val contentDescription = navigationIconContentDescription.orEmpty()
    assert(contentDescription.isNotBlank()) {
        "WooTopAppBar navigationIconContentDescription must not be blank when navigationIcon is set"
    }

    return {
        WooTopAppBarNavigationIcon(
            imageVector = navigationIcon,
            contentDescription = contentDescription,
            onClick = navigationClick,
        )
    }
}

/** Scope for content emitted into the [WooTopAppBar] action slot. */
interface WooTopAppBarActionsScope : RowScope {
    @Composable
    fun IconAction(
        imageVector: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    )

    @Composable
    fun TextAction(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    )

    /** Standard ellipsis trigger anchoring a [WooOverflowMenu]. */
    @Composable
    fun OverflowAction(
        contentDescription: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    )
}

private class WooTopAppBarActionsScopeImpl(
    rowScope: RowScope,
) : WooTopAppBarActionsScope, RowScope by rowScope {
    @Composable
    override fun IconAction(
        imageVector: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        require(contentDescription.isNotBlank()) {
            "WooTopAppBar icon action contentDescription must not be blank"
        }
        WooIconButton(
            imageVector = imageVector,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            emphasis = WOO_TOP_APP_BAR_ACTION_ICON_EMPHASIS,
        )
    }

    @Composable
    override fun TextAction(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        require(text.isNotBlank()) {
            "WooTopAppBar text action text must not be blank"
        }
        WooTopAppBarTextAction(text, onClick, enabled, modifier)
    }

    @Composable
    override fun OverflowAction(
        contentDescription: String,
        modifier: Modifier,
        enabled: Boolean,
        content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    ) {
        WooOverflowMenu(
            trigger = { onClick ->
                IconAction(
                    imageVector = WooIcons.Regular.Ellipsis,
                    contentDescription = contentDescription,
                    onClick = onClick,
                    modifier = modifier,
                    enabled = enabled,
                )
            },
            content = content,
        )
    }
}

@Composable
private fun WooTopAppBarScopedActions(actions: @Composable WooTopAppBarActionsScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooTopAppBarActionsScopeImpl(this).actions()
    }
}

@Composable
private fun WooTopAppBarTextAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = wooTopAppBarActionColors(WooTheme.colors)
    TextButton(
        onClick = onClick,
        modifier = modifier.widthIn(max = ACTION_TEXT_MAX_WIDTH),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = colors.contentColor,
            disabledContentColor = colors.disabledContentColor,
        ),
    ) {
        Text(
            text = text,
            style = WooTheme.text.labelLarge.emphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal data class WooTopAppBarActionColors(
    val contentColor: Color,
    val disabledContentColor: Color,
)

internal fun wooTopAppBarActionColors(colors: WooColors): WooTopAppBarActionColors =
    WooTopAppBarActionColors(
        contentColor = colors.surface.onDefault,
        disabledContentColor = colors.surface.onVariantLowest,
    )

@Composable
private fun WooTopAppBarNavigationIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    WooIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
    ) {
        TopAppBarIcon(
            imageVector = imageVector,
            contentDescription = null,
            autoMirror = true,
        )
    }
}

@Composable
private fun TopAppBarIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    autoMirror: Boolean,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(WooTheme.iconSize.size24)
            .autoMirrorWhenNeeded(imageVector, autoMirror),
    )
}

@Composable
private fun Modifier.autoMirrorWhenNeeded(
    imageVector: ImageVector,
    enabled: Boolean,
): Modifier =
    if (!enabled || imageVector.autoMirror || LocalLayoutDirection.current != LayoutDirection.Rtl) {
        this
    } else {
        scale(scaleX = -1f, scaleY = 1f)
    }

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun WooTopAppBarSmallPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Store settings",
            navigationIcon = WooIcons.Regular.AngleLeft,
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = {
                IconAction(
                    imageVector = WooIcons.Regular.ArrowUpRight,
                    contentDescription = "Open",
                    onClick = {},
                )
            },
        )
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun WooTopAppBarMediumPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Products",
            supportingText = "12 products",
            size = WooTopAppBarSize.Medium,
            windowInsets = WindowInsets(0),
            actions = {
                IconAction(
                    imageVector = WooIcons.Regular.ArrowUpRight,
                    contentDescription = "Open",
                    onClick = {},
                )
            },
        )
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Centered", showBackground = true)
@Composable
private fun WooTopAppBarCenteredPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Centered title",
            titleAlignment = WooTopAppBarTitleAlignment.Center,
            windowInsets = WindowInsets(0),
        )
    }
}

@Suppress("UnusedPrivateMember")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "RTL", locale = "ar", showBackground = true)
@Composable
private fun WooTopAppBarRtlPreview() {
    WooDesignSystemTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            WooTopAppBar(
                title = "RTL title",
                navigationIcon = WooIcons.Regular.AngleLeft,
                navigationIconContentDescription = "Back",
                onNavigationClick = {},
                windowInsets = WindowInsets(0),
            )
        }
    }
}

internal const val WOO_TOP_APP_BAR_DIVIDER_TEST_TAG = "woo_top_app_bar_divider"

private const val SCROLLED_FRACTION_THRESHOLD = 0.01f
private val ACTION_TEXT_MAX_WIDTH = 136.dp
internal val WOO_TOP_APP_BAR_ACTION_ICON_EMPHASIS = WooIconButtonEmphasis.Neutral
