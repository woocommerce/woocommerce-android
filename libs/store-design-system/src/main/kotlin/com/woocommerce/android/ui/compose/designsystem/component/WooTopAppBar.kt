package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.AngleLeft
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: List<WooTopAppBarAction> = emptyList(),
) {
    WooTopAppBarLayout(
        title = { TopAppBarTitle(title) },
        modifier = modifier,
        navigationIcon = topAppBarNavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationClick,
        ),
        windowInsets = windowInsets,
        actions = {
            WooTopAppBarActions(actions)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: List<WooTopAppBarAction> = emptyList(),
) {
    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
        actions = {
            WooTopAppBarActions(actions)
        },
    )
}

/**
 * Prefer descriptor [WooTopAppBarAction] actions for screens that only need standard actions. Scoped [actions] are
 * an escape hatch for screens that additionally need inline custom content — a counter, a progress indicator, or a
 * menu anchored to its trigger — kept in source order alongside [WooTopAppBarActionsScope.iconAction] and
 * [WooTopAppBarActionsScope.textAction]. The design system owns the spacing between emitted actions, so callers
 * must not wrap them in their own spacing row.
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
    actions: @Composable WooTopAppBarActionsScope.() -> Unit,
) {
    WooTopAppBarLayout(
        title = { TopAppBarTitle(title) },
        modifier = modifier,
        navigationIcon = topAppBarNavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationClick,
        ),
        windowInsets = windowInsets,
        actions = { WooTopAppBarScopedActions(actions) },
    )
}

/**
 * Prefer descriptor actions for screens that only need standard actions. Scoped actions are an escape hatch for
 * screens that additionally need inline custom content kept in source order; the design system owns the spacing
 * between emitted actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit,
) {
    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
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
    actions: @Composable RowScope.() -> Unit,
) {
    val edgeInset = dimensionResource(R.dimen.woo_ds_toolbar_edge_padding)
    val addedHorizontalInset = edgeInset -
        MATERIAL_TOP_APP_BAR_HORIZONTAL_PADDING -
        MATERIAL_ICON_BUTTON_VISUAL_TOUCH_TARGET_PADDING
    val titleColor = WooTheme.colors.surface.onDefault

    Box(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = {
                CompositionLocalProvider(LocalContentColor provides titleColor) {
                    ProvideTextStyle(WooTheme.text.bodyLarge.strong) {
                        title()
                    }
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = windowInsets.add(
                WindowInsets(
                    left = addedHorizontalInset,
                    right = addedHorizontalInset,
                ),
            ),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = WooTheme.colors.surface.bright,
                navigationIconContentColor = WooTheme.colors.surface.onDefault,
                titleContentColor = titleColor,
                actionIconContentColor = WooTheme.colors.primary,
            ),
        )
        WooDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TopAppBarTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        color = WooTheme.colors.surface.onDefault,
        style = WooTheme.text.bodyLarge.strong,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun topAppBarNavigationIcon(
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String?,
    onNavigationClick: (() -> Unit)?,
): @Composable () -> Unit {
    if (navigationIcon == null) {
        return {}
    }

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

/**
 * Scope for content emitted into the [WooTopAppBar] action slot.
 *
 * The design system places emitted content in one spacing container that applies [WooTheme.spacing].space1 between
 * direct child layout bounds. For [iconAction], those are the 48dp interactive bounds around the visible 40dp
 * outline, so the visible outline gap also includes both 4dp visual insets. Conditional content that emits no child
 * adds no gap. Because the scope extends [RowScope], callers can emit arbitrary composables in source order.
 */
interface WooTopAppBarActionsScope : RowScope {
    @Composable
    fun iconAction(
        imageVector: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    )

    @Composable
    fun textAction(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    )

    /**
     * Standard outlined ellipsis trigger anchoring a [WooOverflowMenu]. [content] receives the callback that closes
     * the menu; invoke it before running a selected action. [modifier] applies to the trigger.
     */
    @Composable
    fun overflowAction(
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
    override fun iconAction(
        imageVector: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        require(contentDescription.isNotBlank()) {
            "WooTopAppBar icon action contentDescription must not be blank"
        }
        WooOutlinedIconButton(
            imageVector = imageVector,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }

    @Composable
    override fun textAction(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        require(text.isNotBlank()) {
            "WooTopAppBar text action text must not be blank"
        }
        WooTopAppBarTextAction(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }

    @Composable
    override fun overflowAction(
        contentDescription: String,
        modifier: Modifier,
        enabled: Boolean,
        content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    ) {
        WooOverflowMenu(
            trigger = { onClick ->
                iconAction(
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
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooTopAppBarActionsScopeImpl(this).actions()
    }
}

@Composable
private fun WooTopAppBarActions(actions: List<WooTopAppBarAction>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            RenderWooTopAppBarAction(action)
        }
    }
}

@Composable
private fun RenderWooTopAppBarAction(action: WooTopAppBarAction) {
    when (action) {
        is WooTopAppBarAction.Icon -> {
            WooOutlinedIconButton(
                imageVector = action.imageVector,
                contentDescription = action.contentDescription,
                onClick = action.onClick,
                enabled = action.enabled,
            )
        }

        is WooTopAppBarAction.Text -> {
            WooTopAppBarTextAction(
                text = action.text,
                onClick = action.onClick,
                enabled = action.enabled,
            )
        }
    }
}

@Composable
private fun WooTopAppBarTextAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.widthIn(max = ACTION_TEXT_MAX_WIDTH),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = WooTheme.colors.primary,
            disabledContentColor = WooTheme.colors.surface.onVariantLowest,
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

@Composable
private fun WooTopAppBarNavigationIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    WooOutlinedIconButton(
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
            .size(WooTheme.iconSize.size18)
            .autoMirrorWhenNeeded(
                imageVector = imageVector,
                enabled = autoMirror,
            ),
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
@PreviewLightDark
@Composable
private fun WooTopAppBarDesignSystemPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Store settings",
            navigationIcon = WooIcons.Regular.AngleLeft,
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = listOf(
                WooTopAppBarAction.Icon(
                    imageVector = WooIcons.Regular.ArrowUpRight,
                    contentDescription = "Open",
                    onClick = {},
                ),
                WooTopAppBarAction.Text(
                    text = "Done",
                    onClick = {},
                ),
            ),
        )
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTopAppBarLongTitlePreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "A very long top app bar title that should truncate before actions",
            navigationIcon = WooIcons.Regular.AngleLeft,
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = listOf(
                WooTopAppBarAction.Text(
                    text = "Save",
                    onClick = {},
                ),
                WooTopAppBarAction.Icon(
                    imageVector = WooIcons.Regular.ArrowUpRight,
                    contentDescription = "Open",
                    onClick = {},
                ),
            ),
        )
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTopAppBarLongTextActionPreview() {
    WooDesignSystemTheme {
        Column(modifier = Modifier.width(PREVIEW_PHONE_WIDTH)) {
            WooTopAppBar(
                title = "Products",
                navigationIcon = WooIcons.Regular.AngleLeft,
                navigationIconContentDescription = "Back",
                onNavigationClick = {},
                windowInsets = WindowInsets(0),
                actions = listOf(
                    WooTopAppBarAction.Text(
                        text = "Complete setup changes",
                        onClick = {},
                    ),
                    WooTopAppBarAction.Icon(
                        imageVector = WooIcons.Regular.ArrowUpRight,
                        contentDescription = "Open",
                        onClick = {},
                    ),
                ),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTopAppBarScopedActionsPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Product",
            navigationIcon = WooIcons.Regular.AngleLeft,
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = {
                textAction(
                    text = "Save",
                    onClick = {},
                )
                iconAction(
                    imageVector = WooIcons.Regular.ArrowUpRight,
                    contentDescription = "Open",
                    onClick = {},
                )
                overflowAction(contentDescription = "More options") { dismiss ->
                    WooOverflowMenuItem(text = "Duplicate", onClick = dismiss)
                }
            },
        )
    }
}

@Suppress("UnusedPrivateMember")
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

private val ACTION_TEXT_MAX_WIDTH = 136.dp
private val PREVIEW_PHONE_WIDTH = 360.dp

// Material3's top app bar adds this private slot padding internally; subtract it from our desired edge inset.
private val MATERIAL_TOP_APP_BAR_HORIZONTAL_PADDING = 4.dp

// Material3 centers the 40dp outlined icon button inside a 48dp touch target, adding another 4dp visual inset.
private val MATERIAL_ICON_BUTTON_VISUAL_TOUCH_TARGET_PADDING = 4.dp
