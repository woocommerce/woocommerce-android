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

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit),
    navigationIcon: ImageVector = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    onActionButtonClick: (() -> Unit),
    actionButtonIcon: ImageVector = ImageVector.vectorResource(id = drawable.ic_help_24dp),
    actionIconContentDescription: String = stringResource(id = string.help)
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
    onNavigationButtonClick: (() -> Unit),
    navigationIcon: ImageVector? = Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = string.back),
    onActionButtonClick: (() -> Unit)? = null,
    actionButtonText: String? = null
) {
    Toolbar(
        modifier = modifier,
        title = { Text(title) },
        onNavigationButtonClick = onNavigationButtonClick,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        actions = {
            if (onActionButtonClick != null && actionButtonText != null) {
                TextButton(onClick = onActionButtonClick) {
                    Text(text = actionButtonText)
                }
            } else if (onActionButtonClick == null && actionButtonText != null || onActionButtonClick != null) {
                error("Both actionButtonText and onActionButtonClick must be set")
            }
        }
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String = stringResource(id = string.back),
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = title,
        navigationIcon = {
            if (onNavigationButtonClick != null && navigationIcon != null) {
                IconButton(onClick = onNavigationButtonClick) {
                    Icon(
                        navigationIcon,
                        contentDescription = navigationIconContentDescription
                    )
                }
            } else if (onNavigationButtonClick == null && navigationIcon != null || onNavigationButtonClick != null) {
                error("Both onNavigationButtonClick and navigationIcon must be set")
            }
        },
        actions = actions,
        elevation = 0.dp,
        modifier = modifier
    )
}
