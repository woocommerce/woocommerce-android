package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@Composable
fun <T> DashboardFilterableCardHeader(
    title: String,
    currentFilter: T,
    filterList: List<T>,
    onFilterSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    mapper: @Composable (T) -> String = { it.toString() }
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = WooTheme.padding.padding5,
        )
    ) {
        Text(
            text = title,
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onDefault,
        )

        Text(
            text = mapper(currentFilter),
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onDefault,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box {
            var isMenuExpanded by remember { mutableStateOf(false) }
            WooIconButton(
                onClick = { isMenuExpanded = true },
                contentDescription = stringResource(id = R.string.dashboard_filter_menu_content_description),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_filter),
                    contentDescription = null,
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                containerColor = WooTheme.colors.surface.default,
                modifier = Modifier
                    .defaultMinSize(minWidth = 250.dp)
            ) {
                filterList.forEach {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                            ) {
                                Text(
                                    text = mapper(it),
                                    style = WooTheme.text.bodyLarge.regular,
                                    color = WooTheme.colors.surface.onDefault,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (currentFilter == it) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_menu_check),
                                        contentDescription = stringResource(
                                            id = androidx.compose.ui.R.string.selected
                                        ),
                                        tint = WooTheme.colors.primary,
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(WooTheme.iconSize.size24))
                                }
                            }
                        },
                        onClick = {
                            onFilterSelected(it)
                            isMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun DashboardFilterableCardHeaderPreview() {
    val filters = remember {
        listOf("Filter 1", "Filter 2", "Filter 3")
    }
    var currentFilter by remember { mutableStateOf("Filter 1") }
    WooDesignSystemThemeWithBackground {
        DashboardFilterableCardHeader(
            title = "Title",
            currentFilter = currentFilter,
            filterList = filters,
            onFilterSelected = { currentFilter = it },
            modifier = Modifier
        )
    }
}
