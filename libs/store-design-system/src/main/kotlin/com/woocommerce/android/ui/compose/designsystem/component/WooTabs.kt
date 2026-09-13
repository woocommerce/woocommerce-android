package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Box
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.List
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun WooTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier
            .fillMaxWidth()
            .height(WooTabsDefaults.RowHeight),
        containerColor = WooTheme.colors.surface.bright,
        contentColor = WooTheme.colors.surface.onDefault,
        indicator = {
            WooTabIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(
                        selectedTabIndex = selectedTabIndex,
                        matchContentSize = false,
                    )
                    .fillMaxWidth(),
            )
        },
        divider = { WooTabRowDivider() },
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
    val contentColor = when {
        !enabled -> WooTheme.colors.surface.onVariantLowest
        else -> WooTheme.colors.surface.onDefault
    }

    Box(
        modifier = modifier
            .height(WooTabsDefaults.RowHeight)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            WooTabContent(
                text = text,
                icon = icon,
                modifier = Modifier
                    .align(if (icon == null) Alignment.BottomCenter else Alignment.TopCenter)
                    .padding(horizontal = WooTheme.padding.padding5)
                    .then(
                        if (icon == null) {
                            Modifier.padding(bottom = WooTheme.padding.padding6)
                        } else {
                            Modifier
                        }
                    ),
            )
        }
    }
}

@Composable
private fun WooTabContent(
    text: String,
    icon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
    ) {
        icon?.let {
            Box(
                modifier = Modifier.size(WooTabsDefaults.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                it()
            }
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = WooTheme.text.labelMedium.emphasized,
            color = LocalContentColor.current,
        )
    }
}

@Composable
private fun WooTabRowDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WooTheme.stroke.extraThin)
            .background(WooTheme.colors.tintLayers.onSurface.opacity16),
    )
}

@Composable
private fun WooTabIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(WooTheme.stroke.thick)
            .background(
                color = WooTheme.colors.primary,
                shape = RoundedCornerShape(WooTheme.radius.small),
            ),
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooTabsPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooTabsDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooTabsDemo(
    modifier: Modifier = Modifier,
) {
    var selectedIconTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedTextTabIndex by rememberSaveable { mutableIntStateOf(1) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        WooTabRow(selectedTabIndex = selectedIconTabIndex) {
            WooTab(
                selected = selectedIconTabIndex == TAB_PRODUCTS_INDEX,
                onClick = { selectedIconTabIndex = TAB_PRODUCTS_INDEX },
                text = "Products",
                icon = {
                    Icon(
                        imageVector = WooIcons.Regular.Box,
                        contentDescription = null,
                    )
                },
            )
            WooTab(
                selected = selectedIconTabIndex == TAB_ORDERS_INDEX,
                onClick = { selectedIconTabIndex = TAB_ORDERS_INDEX },
                text = "Orders",
                icon = {
                    Icon(
                        imageVector = WooIcons.Regular.List,
                        contentDescription = null,
                    )
                },
            )
            WooTab(
                selected = selectedIconTabIndex == TAB_MORE_INDEX,
                onClick = { selectedIconTabIndex = TAB_MORE_INDEX },
                text = "More",
                icon = {
                    Icon(
                        imageVector = WooIcons.Regular.CircleInfo,
                        contentDescription = null,
                    )
                },
            )
        }
        WooTabRow(selectedTabIndex = selectedTextTabIndex) {
            WooTab(
                selected = selectedTextTabIndex == TAB_PRODUCTS_INDEX,
                onClick = { selectedTextTabIndex = TAB_PRODUCTS_INDEX },
                text = "Products",
            )
            WooTab(
                selected = selectedTextTabIndex == TAB_ORDERS_INDEX,
                onClick = { selectedTextTabIndex = TAB_ORDERS_INDEX },
                text = "Orders",
            )
            WooTab(
                selected = selectedTextTabIndex == TAB_MORE_INDEX,
                onClick = { selectedTextTabIndex = TAB_MORE_INDEX },
                text = "More",
            )
        }
    }
}

private object WooTabsDefaults {
    val RowHeight = 60.dp
    val IconSize = 18.dp
}

private const val TAB_PRODUCTS_INDEX = 0
private const val TAB_ORDERS_INDEX = 1
private const val TAB_MORE_INDEX = 2
