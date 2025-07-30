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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarM3(
    modifier: Modifier = Modifier,
    title: String = "",
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    actions: @Composable RowScope.() -> Unit = {}
) {
    ToolbarM3(
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
fun ToolbarM3(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    onNavigationButtonClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String = stringResource(id = R.string.back),
    windowInsets: WindowInsets = WindowInsets(0),
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        windowInsets = windowInsets,
        title = title,
        navigationIcon = {
            if (navigationIcon != null) {
                if (onNavigationButtonClick == null) {
                    error("Please make sure to set onNavigationButtonClick when having a navigation icon")
                }
                IconButton(onClick = onNavigationButtonClick) {
                    Icon(
                        navigationIcon,
                        contentDescription = navigationIconContentDescription
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(id = R.color.color_toolbar),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}
