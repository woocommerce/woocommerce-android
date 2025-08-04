package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.woocommerce.android.R
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.autoMirror

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarWithHelpButton(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    onHelpButtonClick: (() -> Unit)
) {
    Toolbar(
        modifier = modifier,
        title = title,
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        actionButtonIcon = ImageVector.vectorResource(id = drawable.ic_help_24dp),
        onActionButtonClick = onHelpButtonClick,
        actionIconContentDescription = stringResource(id = string.help),
        windowInsets = windowInsets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit),
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
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
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actionButtonIcon: ImageVector,
    onActionButtonClick: (() -> Unit),
    actionIconContentDescription: String
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = {
            IconButton(onClick = onActionButtonClick) {
                Icon(
                    imageVector = actionButtonIcon,
                    contentDescription = actionIconContentDescription,
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    onActionButtonClick: (() -> Unit),
    actionButtonText: String
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        windowInsets = windowInsets,
        actions = {
            TextButton(onClick = onActionButtonClick) {
                Text(text = actionButtonText)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String = stringResource(id = string.back),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(id = R.color.color_toolbar),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = title,
        navigationIcon = {
            if (navigationIcon != null) {
                if (onNavigationButtonClick == null) {
                    error("Please make sure to set onNavigationButtonClick when having a navigation icon")
                }
                IconButton(onClick = onNavigationButtonClick) {
                    Icon(
                        navigationIcon,
                        contentDescription = navigationIconContentDescription,
                        modifier = Modifier.then(
                            if (navigationIcon.autoMirror) Modifier else Modifier.autoMirror()
                        )
                    )
                }
            }
        },
        actions = actions,
        modifier = modifier
    )
}
