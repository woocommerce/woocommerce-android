package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags
import java.util.Calendar
import java.util.Date

@Composable
fun DashboardDateRangeHeader(
    rangeSelection: StatsTimeRangeSelection,
    dateFormatted: String,
    onCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = WooTheme.padding.padding5,
        )
    ) {
        Text(
            text = rangeSelection.selectionType.title,
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onDefault,
        )

        val isCustomRange = rangeSelection.selectionType == SelectionType.CUSTOM
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .wrapContentSize(align = Alignment.CenterStart)
                .then(if (isCustomRange) Modifier.clickable(onClick = onCustomRangeClick) else Modifier)
                .padding(WooTheme.padding.padding3)
        ) {
            Text(
                text = dateFormatted,
                style = WooTheme.text.bodyMedium.regular,
                color = if (isCustomRange) {
                    WooTheme.colors.container.onSecondaryContainer
                } else {
                    WooTheme.colors.surface.onDefault
                },
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isCustomRange) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_pencil),
                    contentDescription = null,
                    tint = WooTheme.colors.container.onSecondaryContainer,
                    modifier = Modifier.size(WooTheme.iconSize.size20),
                )
            }
        }

        Box {
            var isMenuExpanded by remember { mutableStateOf(false) }
            WooIconButton(
                onClick = { isMenuExpanded = true },
                contentDescription = stringResource(
                    id = R.string.dashboard_stats_edit_granularity_content_description
                ),
                modifier = Modifier.testTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_BUTTON),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_date_range_24dp),
                    contentDescription = null,
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                containerColor = WooTheme.colors.surface.default,
                modifier = Modifier
                    .defaultMinSize(minWidth = 250.dp)
                    .testTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_MENU)
            ) {
                DashboardViewModel.SUPPORTED_RANGES_ON_MY_STORE_TAB.forEach {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                            ) {
                                Text(
                                    text = it.title,
                                    style = WooTheme.text.bodyLarge.regular,
                                    color = WooTheme.colors.surface.onDefault,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (rangeSelection.selectionType == it) {
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
                            onTabSelected(it)
                            isMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun DashboardDateRangeHeaderPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardDateRangeHeader(
            rangeSelection = SelectionType.TODAY.generateSelectionData(
                Date(),
                Date(),
                Calendar.getInstance(),
                LocalLocale.current.platformLocale
            ),
            dateFormatted = "Today",
            onCustomRangeClick = {},
            onTabSelected = {}
        )
    }
}

private val SelectionType.title: String
    @Composable
    get() = when (this) {
        SelectionType.TODAY -> stringResource(id = R.string.today)
        SelectionType.WEEK_TO_DATE -> stringResource(id = R.string.this_week)
        SelectionType.MONTH_TO_DATE -> stringResource(id = R.string.this_month)
        SelectionType.YEAR_TO_DATE -> stringResource(id = R.string.this_year)
        SelectionType.CUSTOM -> stringResource(id = R.string.date_timeframe_custom)
        else -> error("Invalid selection type")
    }
