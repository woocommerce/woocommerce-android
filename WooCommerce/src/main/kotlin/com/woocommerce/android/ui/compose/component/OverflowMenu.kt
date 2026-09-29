package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.component.WooOverflowMenuItem

@Composable
fun <T> WCOverflowMenu(
    items: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    mapper: @Composable (T) -> String = { it.toString() },
    itemColor: @Composable (T) -> Color = { LocalContentColor.current },
    tint: Color = MaterialTheme.colorScheme.primary
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_more_vert),
                contentDescription = stringResource(R.string.more_menu),
                tint = tint
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mapper(item),
                            color = itemColor(item),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        showMenu = false
                        onSelected(item)
                    }
                )
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                }
            }
        }
    }
}

/**
 * Bridge to the design system [WooOverflowMenuItem] for Material themed screens, so they can fill design system
 * overflow menus without importing the design system alongside the legacy theme.
 */
@Composable
fun WCOverflowMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    WooOverflowMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        isDestructive = isDestructive,
        enabled = enabled,
        trailingIcon = trailingIcon,
    )
}

@Preview
@Composable
fun WCOverflowMenuPreview() {
    WCOverflowMenu(
        items = listOf("Option 1", "Option 2", "Option 3"),
        onSelected = {}
    )
}
