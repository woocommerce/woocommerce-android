package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.autoMirror
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.DefaultWooStroke
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
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigationIconSlot: @Composable () -> Unit = navigationIcon?.let { imageVector ->
        val navigationClick = requireNotNull(onNavigationClick) {
            "WooTopAppBar requires onNavigationClick when navigationIcon is set"
        }
        val contentDescription = requireNotNull(navigationIconContentDescription) {
            "WooTopAppBar navigationIconContentDescription must not be null when navigationIcon is set"
        }
        require(contentDescription.isNotBlank()) {
            "WooTopAppBar navigationIconContentDescription must not be blank when navigationIcon is set"
        }

        val slot: @Composable (() -> Unit) = {
            WooTopAppBarNavigationIcon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                onClick = navigationClick,
            )
        }
        slot
    } ?: {}

    WooTopAppBar(
        title = {
            TopAppBarTitle(title)
        },
        modifier = modifier,
        navigationIcon = navigationIconSlot,
        windowInsets = windowInsets,
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = {
                // We are intentionally using a smaller spacing for actions than Figma because the icons have padding
                // because of touch target size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            },
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
private fun WooTopAppBarNavigationIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(MIN_TOUCH_TARGET_SIZE),
    ) {
        Box(
            modifier = Modifier
                .size(NAVIGATION_BUTTON_VISUAL_SIZE)
                .border(
                    DefaultWooStroke.extraThin,
                    WooTheme.colors.outlineVariant,
                    MaterialTheme.shapes.large,
                )
                .clip(MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = WooTheme.colors.surface.onDefault,
                modifier = Modifier
                    .size(NAVIGATION_ICON_SIZE)
                    .then(
                        if (imageVector.autoMirror) {
                            Modifier
                        } else {
                            Modifier.autoMirror()
                        },
                    ),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTopAppBarPreview() {
    WooDesignSystemTheme {
        WooTopAppBar(
            title = "Store settings",
            navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
            navigationIconContentDescription = "Back",
            onNavigationClick = {},
            windowInsets = WindowInsets(0),
            actions = {
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_help_24dp),
                    contentDescription = "Help",
                    onClick = {},
                )
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = "Open",
                    onClick = {},
                    emphasis = WooIconButtonEmphasis.Primary,
                )
            },
        )
    }
}

private val MIN_TOUCH_TARGET_SIZE = 48.dp
private val NAVIGATION_BUTTON_VISUAL_SIZE = 40.dp
private val NAVIGATION_ICON_SIZE = 18.dp
