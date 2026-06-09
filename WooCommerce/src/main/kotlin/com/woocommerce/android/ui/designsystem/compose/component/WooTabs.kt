package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = WooTheme.colors.surface.default,
        contentColor = WooTheme.colors.surface.onDefault,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false),
                width = Dp.Unspecified,
                color = WooTheme.colors.primary,
            )
        },
        tabs = tabs,
    )
}

@Composable
fun WooTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selectedContentColor = WooTheme.colors.primary,
        unselectedContentColor = WooTheme.colors.surface.onVariant,
        icon = icon,
        text = {
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = WooTheme.text.labelLarge.emphasized,
            )
        },
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTabsPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooTabRow(selectedTabIndex = 0) {
                    WooTab(
                        selected = true,
                        onClick = {},
                        text = "Products",
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_products_list),
                                contentDescription = null,
                            )
                        },
                    )
                    WooTab(
                        selected = false,
                        onClick = {},
                        text = "Orders",
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_orders_list),
                                contentDescription = null,
                            )
                        },
                    )
                    WooTab(selected = false, onClick = {}, text = "More")
                }
            }
        }
    }
}
