package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.autoMirror
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.WooTopAppBarAppearance
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

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
    val appearance = WooTheme.topAppBarAppearance

    WooTopAppBarLayout(
        title = {
            TopAppBarTitle(title, appearance)
        },
        modifier = modifier,
        navigationIcon = topAppBarNavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationClick,
            appearance = appearance,
        ),
        windowInsets = windowInsets,
        appearance = appearance,
        actions = {
            WooTopAppBarActions(actions, appearance)
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
    val appearance = WooTheme.topAppBarAppearance

    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
        appearance = appearance,
        actions = {
            WooTopAppBarActions(actions, appearance)
        },
    )
}

/**
 * Prefer descriptor [actions] for migrated screens. Raw actions are an advanced escape hatch and are not
 * appearance-adaptive; the supplied composables render as provided inside the selected top app bar layout.
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
    actions: @Composable RowScope.() -> Unit,
) {
    val appearance = WooTheme.topAppBarAppearance

    WooTopAppBarLayout(
        title = {
            TopAppBarTitle(title, appearance)
        },
        modifier = modifier,
        navigationIcon = topAppBarNavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationClick,
            appearance = appearance,
        ),
        windowInsets = windowInsets,
        appearance = appearance,
        actions = actions,
    )
}

/**
 * Prefer descriptor actions for migrated screens. Raw actions are an advanced escape hatch and are not
 * appearance-adaptive; the supplied composables render as provided inside the selected top app bar layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit,
) {
    val appearance = WooTheme.topAppBarAppearance

    WooTopAppBarLayout(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        windowInsets = windowInsets,
        appearance = appearance,
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
    appearance: WooTopAppBarAppearance,
    actions: @Composable RowScope.() -> Unit,
) {
    when (appearance) {
        WooTopAppBarAppearance.DesignSystem -> {
            Column(modifier = modifier) {
                CenterAlignedTopAppBar(
                    title = title,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = windowInsets,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = WooTheme.colors.surface.default,
                        titleContentColor = WooTheme.colors.surface.onDefault,
                        navigationIconContentColor = WooTheme.colors.surface.onDefault,
                        actionIconContentColor = WooTheme.colors.primary,
                    ),
                )
                WooDivider()
            }
        }

        WooTopAppBarAppearance.LegacyCompatible -> {
            TopAppBar(
                modifier = modifier,
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.color_toolbar),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun TopAppBarTitle(
    title: String,
    appearance: WooTopAppBarAppearance,
    modifier: Modifier = Modifier,
) {
    when (appearance) {
        WooTopAppBarAppearance.DesignSystem -> {
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

        WooTopAppBarAppearance.LegacyCompatible -> {
            Text(
                text = title,
                modifier = modifier,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun topAppBarNavigationIcon(
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String?,
    onNavigationClick: (() -> Unit)?,
    appearance: WooTopAppBarAppearance,
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
            appearance = appearance,
        )
    }
}

@Composable
private fun WooTopAppBarActions(
    actions: List<WooTopAppBarAction>,
    appearance: WooTopAppBarAppearance,
) {
    when (appearance) {
        WooTopAppBarAppearance.DesignSystem -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { action ->
                    RenderWooTopAppBarAction(action, appearance)
                }
            }
        }

        WooTopAppBarAppearance.LegacyCompatible -> {
            actions.forEach { action ->
                RenderWooTopAppBarAction(action, appearance)
            }
        }
    }
}

@Composable
private fun RenderWooTopAppBarAction(
    action: WooTopAppBarAction,
    appearance: WooTopAppBarAppearance,
) {
    when (action) {
        is WooTopAppBarAction.Icon -> {
            when (appearance) {
                WooTopAppBarAppearance.DesignSystem -> {
                    WooOutlinedIconButton(
                        imageVector = action.imageVector,
                        contentDescription = action.contentDescription,
                        onClick = action.onClick,
                        enabled = action.enabled,
                    )
                }

                WooTopAppBarAppearance.LegacyCompatible -> {
                    WooTopAppBarPlainIconButton(
                        imageVector = action.imageVector,
                        contentDescription = action.contentDescription,
                        onClick = action.onClick,
                        enabled = action.enabled,
                        autoMirror = false,
                    )
                }
            }
        }

        is WooTopAppBarAction.Text -> {
            WooTopAppBarTextAction(
                text = action.text,
                onClick = action.onClick,
                enabled = action.enabled,
                appearance = appearance,
            )
        }
    }
}

@Composable
private fun WooTopAppBarTextAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    appearance: WooTopAppBarAppearance,
) {
    when (appearance) {
        WooTopAppBarAppearance.DesignSystem -> {
            TextButton(
                onClick = onClick,
                modifier = Modifier.widthIn(max = ACTION_TEXT_MAX_WIDTH),
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = WooTheme.colors.primary,
                    disabledContentColor = WooTheme.colors.surface.onLowest,
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

        WooTopAppBarAppearance.LegacyCompatible -> {
            TextButton(
                onClick = onClick,
                modifier = Modifier.widthIn(max = ACTION_TEXT_MAX_WIDTH),
                enabled = enabled,
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WooTopAppBarNavigationIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    appearance: WooTopAppBarAppearance,
) {
    when (appearance) {
        WooTopAppBarAppearance.DesignSystem -> {
            WooOutlinedIconButton(
                onClick = onClick,
                contentDescription = contentDescription,
            ) {
                TopAppBarIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    size = DESIGN_SYSTEM_ICON_SIZE,
                    autoMirror = true,
                )
            }
        }

        WooTopAppBarAppearance.LegacyCompatible -> {
            WooTopAppBarPlainIconButton(
                imageVector = imageVector,
                contentDescription = contentDescription,
                onClick = onClick,
                autoMirror = true,
            )
        }
    }
}

@Composable
private fun WooTopAppBarPlainIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    autoMirror: Boolean,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(LEGACY_ICON_BUTTON_SIZE),
        enabled = enabled,
    ) {
        TopAppBarIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            size = LEGACY_ICON_SIZE,
            autoMirror = autoMirror,
        )
    }
}

@Composable
private fun TopAppBarIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    size: Dp,
    autoMirror: Boolean,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(size)
            .then(if (autoMirror) imageVector.autoMirrorModifier() else Modifier),
    )
}

private fun ImageVector.autoMirrorModifier(): Modifier =
    if (autoMirror) {
        Modifier
    } else {
        Modifier.autoMirror()
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
private fun WooTopAppBarLegacyCompatiblePreview() {
    WooThemeWithBackground {
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
        WooTopAppBar(
            title = "RTL title",
            navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
        )
    }
}

private val DESIGN_SYSTEM_ICON_SIZE = 18.dp
private val LEGACY_ICON_BUTTON_SIZE = 48.dp
private val LEGACY_ICON_SIZE = 24.dp
private val ACTION_TEXT_MAX_WIDTH = 136.dp
private val PREVIEW_PHONE_WIDTH = 360.dp
