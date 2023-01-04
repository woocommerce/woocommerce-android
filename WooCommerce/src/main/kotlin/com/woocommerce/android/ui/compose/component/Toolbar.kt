package com.woocommerce.android.ui.compose.component

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    onCloseButtonClick: (() -> Unit)? = null,
    onActionButtonClick: (() -> Unit)? = null,
    title: String = "",
    closeButtonIcon: ImageVector = Filled.ArrowBack,
    @DrawableRes actionButtonIcon: Int = drawable.ic_help_24dp,
    actionButtonText: String? = null
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {
            if (title.isNotBlank()) {
                Text(title)
            }
        },
        navigationIcon = {
            if (onCloseButtonClick != null) {
                IconButton(onClick = onCloseButtonClick) {
                    Icon(
                        closeButtonIcon,
                        contentDescription = stringResource(id = string.back)
                    )
                }
            }
        },
        actions = {
            if (onActionButtonClick != null) {
                if (actionButtonText != null) {
                    TextButton(onClick = onActionButtonClick) {
                        Text(text = actionButtonText)
                    }
                } else {
                    IconButton(onClick = onActionButtonClick) {
                        Icon(
                            painter = painterResource(id = actionButtonIcon),
                            contentDescription = stringResource(id = string.help)
                        )
                    }
                }
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}
