package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.modifier.wooPosUserInteraction

@Composable
fun WooPosOverflowMenu(
    modifier: Modifier = Modifier,
    primaryAction: WooPosOverflowPrimaryAction? = null,
    items: List<WooPosOverflowMenuItem>,
    contentDescription: String = stringResource(R.string.more_menu),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (primaryAction != null) {
            WooPosButtonSmall(
                text = primaryAction.label,
                onClick = primaryAction.onClick
            )
        }

        if (items.isNotEmpty()) {
            OverflowDropdown(items = items, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun OverflowDropdown(items: List<WooPosOverflowMenuItem>, contentDescription: String) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_more_vert),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            modifier = Modifier.wooPosUserInteraction().background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier.semantics {
                        item.contentDescription?.let { this.contentDescription = it }
                    },
                    text = {
                        WooPosText(
                            text = item.label,
                            style = WooPosTypography.BodyMedium,
                            color = item.color
                        )
                    },
                    onClick = {
                        showMenu = false
                        item.onClick()
                    }
                )
            }
        }
    }
}

data class WooPosOverflowPrimaryAction(
    val label: String,
    val onClick: () -> Unit,
)

data class WooPosOverflowMenuItem(
    val label: String,
    val color: Color = Color.Unspecified,
    val contentDescription: String? = null,
    val onClick: () -> Unit,
)
