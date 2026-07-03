package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import kotlin.math.roundToInt

@Composable
fun WooTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(WooTabsDefaults.RowHeight),
        color = WooTheme.colors.surface.default,
        contentColor = WooTheme.colors.surface.onDefault,
    ) {
        WooTabRowLayout(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxSize(),
            tabs = tabs,
        )
    }
}

@Composable
private fun WooTabRowLayout(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    val horizontalPadding = WooTheme.padding.padding7
    val topPadding = WooTheme.padding.padding7

    SubcomposeLayout(modifier = modifier.selectableGroup()) { constraints ->
        val rowWidth = constraints.maxWidth
        val rowHeight = constraints.maxHeight
        val topPaddingPx = topPadding.toPx().roundToInt()
        val contentHeight = (rowHeight - topPaddingPx).coerceAtLeast(0)
        val tabConstraints = Constraints(
            minWidth = 0,
            maxWidth = rowWidth,
            minHeight = contentHeight,
            maxHeight = contentHeight,
        )
        val tabPlaceables = subcompose(WooTabRowSlot.Tabs, tabs).map { measurable ->
            measurable.measure(tabConstraints)
        }
        val tabLayout = calculateWooTabRowLayout(
            rowWidth = rowWidth,
            horizontalPadding = horizontalPadding.toPx().roundToInt(),
            tabWidths = tabPlaceables.map { it.width },
            selectedTabIndex = selectedTabIndex,
        )
        val dividerPlaceables = tabLayout.dividerSegments.mapIndexed { index, segment ->
            subcompose("${WooTabRowSlot.Divider}-$index") {
                WooTabRowDivider()
            }.single().measure(Constraints.fixedWidth(segment.width))
        }
        val indicatorPlaceable = tabLayout.selectedIndicatorWidth?.let { indicatorWidth ->
            subcompose(WooTabRowSlot.Indicator) {
                WooTabIndicator(modifier = Modifier.fillMaxWidth())
            }.single().measure(Constraints.fixedWidth(indicatorWidth))
        }

        layout(rowWidth, rowHeight) {
            tabPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(tabLayout.tabPositions[index], topPaddingPx)
            }
            dividerPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = tabLayout.dividerSegments[index].position,
                    y = rowHeight - placeable.height,
                )
            }
            indicatorPlaceable?.placeRelative(
                x = checkNotNull(tabLayout.selectedIndicatorPosition),
                y = rowHeight - indicatorPlaceable.height,
            )
        }
    }
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
            .fillMaxHeight()
            .defaultMinSize(minWidth = WooTabsDefaults.TabMinWidth)
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
            .height(WooTheme.stroke.extraThin)
            .background(WooTheme.colors.outlineVariant),
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
    var selectedIconTabIndex by rememberSaveable { mutableStateOf(0) }
    var selectedTextTabIndex by rememberSaveable { mutableStateOf(1) }

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
                        imageVector = ImageVector.vectorResource(R.drawable.ic_menu_products_list),
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
                        imageVector = ImageVector.vectorResource(R.drawable.ic_menu_orders_list),
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
                        imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
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
    val RowHeight = 84.dp
    val TabMinWidth = 134.dp
    val IconSize = 18.dp
}

private const val TAB_PRODUCTS_INDEX = 0
private const val TAB_ORDERS_INDEX = 1
private const val TAB_MORE_INDEX = 2

private enum class WooTabRowSlot {
    Tabs,
    Divider,
    Indicator,
}

internal data class WooTabRowLayoutResult(
    val tabPositions: List<Int>,
    val selectedIndicatorPosition: Int?,
    val selectedIndicatorWidth: Int?,
    val dividerSegments: List<WooTabRowDividerSegment>,
)

internal data class WooTabRowDividerSegment(
    val position: Int,
    val width: Int,
)

internal fun calculateWooTabRowLayout(
    rowWidth: Int,
    horizontalPadding: Int,
    tabWidths: List<Int>,
    selectedTabIndex: Int,
): WooTabRowLayoutResult {
    val totalTabWidth = tabWidths.sum()
    val contentWidth = rowWidth - horizontalPadding * 2
    val firstTabPosition = horizontalPadding + (contentWidth - totalTabWidth) / 2
    var tabPosition = firstTabPosition
    val tabPositions = tabWidths.map { tabWidth ->
        tabPosition.also {
            tabPosition += tabWidth
        }
    }
    val selectedIndicatorPosition = tabPositions.getOrNull(selectedTabIndex)
    val selectedIndicatorWidth = tabWidths.getOrNull(selectedTabIndex)

    return WooTabRowLayoutResult(
        tabPositions = tabPositions,
        selectedIndicatorPosition = selectedIndicatorPosition,
        selectedIndicatorWidth = selectedIndicatorWidth,
        dividerSegments = calculateDividerSegments(
            rowWidth = rowWidth,
            selectedIndicatorPosition = selectedIndicatorPosition,
            selectedIndicatorWidth = selectedIndicatorWidth,
        ),
    )
}

private fun calculateDividerSegments(
    rowWidth: Int,
    selectedIndicatorPosition: Int?,
    selectedIndicatorWidth: Int?,
): List<WooTabRowDividerSegment> {
    if (selectedIndicatorPosition == null || selectedIndicatorWidth == null) {
        return listOf(WooTabRowDividerSegment(position = 0, width = rowWidth))
    }

    val selectedIndicatorEnd = selectedIndicatorPosition + selectedIndicatorWidth
    return listOf(
        WooTabRowDividerSegment(position = 0, width = selectedIndicatorPosition),
        WooTabRowDividerSegment(position = selectedIndicatorEnd, width = rowWidth - selectedIndicatorEnd),
    ).filter { it.width > 0 }
}
