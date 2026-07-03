package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

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
 * Prefer descriptor [actions] for migrated screens. Raw actions are an advanced escape hatch; supplied
 * composables render as provided inside the design-system top app bar layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WooTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit,
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
        actions = actions,
    )
}

/**
 * Prefer descriptor actions for migrated screens. Raw actions are an advanced escape hatch; supplied
 * composables render as provided inside the design-system top app bar layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit,
) {
    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
        actions = actions,
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

    Column(modifier = modifier) {
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
                containerColor = WooTheme.colors.surface.default,
                navigationIconContentColor = WooTheme.colors.surface.onDefault,
                titleContentColor = titleColor,
                actionIconContentColor = WooTheme.colors.primary,
            ),
        )
        WooDivider()
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
    val contentDescription = requireNotNull(navigationIconContentDescription) {
        "WooTopAppBar navigationIconContentDescription must not be null when navigationIcon is set"
    }
    require(contentDescription.isNotBlank()) {
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
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.widthIn(max = ACTION_TEXT_MAX_WIDTH),
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
            .then(if (autoMirror) imageVector.autoMirrorModifier() else Modifier),
    )
}

@Composable
private fun ImageVector.autoMirrorModifier(): Modifier =
    if (autoMirror || LocalLayoutDirection.current != LayoutDirection.Rtl) {
        Modifier
    } else {
        Modifier.scale(scaleX = -1f, scaleY = 1f)
    }

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTopAppBarDesignSystemPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Store settings",
            navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = listOf(
                WooTopAppBarAction.Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
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
            navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = listOf(
                WooTopAppBarAction.Text(
                    text = "Save",
                    onClick = {},
                ),
                WooTopAppBarAction.Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
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
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                navigationIconContentDescription = "Back",
                onNavigationClick = {},
                windowInsets = WindowInsets(0),
                actions = listOf(
                    WooTopAppBarAction.Text(
                        text = "Complete setup changes",
                        onClick = {},
                    ),
                    WooTopAppBarAction.Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                        contentDescription = "Open",
                        onClick = {},
                    ),
                ),
            )
        }
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
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
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
