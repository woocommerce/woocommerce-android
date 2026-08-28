package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.autoMirror
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar
import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBarActionsScope
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarWithHelpButton(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = ImageVector.vectorResource(R.drawable.ic_back_24dp),
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    onHelpButtonClick: (() -> Unit)
) {
    Toolbar(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        actionButtonIcon = ImageVector.vectorResource(id = R.drawable.ic_help_24dp),
        onActionButtonClick = onHelpButtonClick,
        actionIconContentDescription = stringResource(id = R.string.help),
        windowInsets = windowInsets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit),
    navigationIcon: ImageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
) {
    ToolbarWithActions(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = ImageVector.vectorResource(R.drawable.ic_back_24dp),
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    actionButtonIcon: ImageVector,
    onActionButtonClick: (() -> Unit),
    actionIconContentDescription: String
) {
    ToolbarWithActions(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = {
            IconAction(
                imageVector = actionButtonIcon,
                contentDescription = actionIconContentDescription,
                onClick = onActionButtonClick,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = ImageVector.vectorResource(R.drawable.ic_back_24dp),
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {}
) {
    ToolbarWithActions(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = ImageVector.vectorResource(R.drawable.ic_back_24dp),
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    onActionButtonClick: (() -> Unit),
    actionButtonText: String
) {
    ToolbarWithActions(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = {
            TextAction(
                text = actionButtonText,
                onClick = onActionButtonClick,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {}
) {
    ToolbarWithActions(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarWithActions(
    modifier: Modifier,
    title: String,
    onNavigationButtonClick: (() -> Unit)?,
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String,
    windowInsets: WindowInsets,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit = {},
) {
    WooDesignSystemTheme(modifier = modifier) {
        WooTopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationClick = onNavigationButtonClick,
            windowInsets = windowInsets,
            actions = actions,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarWithActions(
    modifier: Modifier,
    title: @Composable () -> Unit,
    onNavigationButtonClick: (() -> Unit)?,
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String,
    windowInsets: WindowInsets,
    actions: @Composable WooTopAppBarActionsScope.() -> Unit,
) {
    WooDesignSystemTheme(modifier = modifier) {
        WooTopAppBar(
            title = title,
            navigationIcon = {
                if (navigationIcon != null) {
                    if (onNavigationButtonClick == null) {
                        error("Please make sure to set onNavigationButtonClick when having a navigation icon")
                    }
                    WooOutlinedIconButton(
                        onClick = onNavigationButtonClick,
                        contentDescription = navigationIconContentDescription,
                    ) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(WooTheme.iconSize.size18)
                                .then(if (navigationIcon.autoMirror) Modifier else Modifier.autoMirror()),
                        )
                    }
                }
            },
            windowInsets = windowInsets,
            actions = actions,
        )
    }
}
