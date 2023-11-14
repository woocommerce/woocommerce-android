package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.autoMirror

@Composable
fun ToolbarWithHelpButton(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
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
        actionIconContentDescription = stringResource(id = string.help)
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit),
    navigationIcon: ImageVector = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back)
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
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
        actions = {
            IconButton(onClick = onActionButtonClick) {
                Icon(
                    imageVector = actionButtonIcon,
                    contentDescription = actionIconContentDescription
                )
            }
        }
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    actions: @Composable RowScope.() -> Unit = {}
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        actions = actions
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    onActionButtonClick: (() -> Unit),
    actionButtonText: String
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        actions = {
            TextButton(onClick = onActionButtonClick) {
                Text(text = actionButtonText)
            }
        }
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String = stringResource(id = string.back),
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
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
                        modifier = Modifier.autoMirror()
                    )
                }
            }
        },
        actions = actions,
        elevation = 0.dp,
        modifier = modifier
    )
}
