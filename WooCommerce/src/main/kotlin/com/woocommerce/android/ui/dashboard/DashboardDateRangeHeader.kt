package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags

@Composable
fun DashboardDateRangeHeader(
    rangeSelection: StatsTimeRangeSelection,
    dateFormatted: String,
    onCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = dimensionResource(id = dimen.major_100)
        )
    ) {
        Text(
            text = rangeSelection.selectionType.title,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
        val isCustomRange = rangeSelection.selectionType == SelectionType.CUSTOM

        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(if (isCustomRange) Modifier.clickable(onClick = onCustomRangeClick) else Modifier)
                .padding(dimensionResource(id = dimen.minor_100))
        ) {
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.body2,
                color = if (isCustomRange) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                }
            )
            if (isCustomRange) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_pencil),
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(dimensionResource(id = dimen.image_minor_40))
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box {
            var isMenuExpanded by remember { mutableStateOf(false) }
            IconButton(
                onClick = { isMenuExpanded = true },
                modifier = Modifier.testTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(
                        id = R.string.dashboard_stats_edit_granularity_content_description
                    ),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier
                    .defaultMinSize(minWidth = 250.dp)
                    .testTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_MENU)
            ) {
                DashboardViewModel.SUPPORTED_RANGES_ON_MY_STORE_TAB.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onTabSelected(it)
                            isMenuExpanded = false
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(dimen.minor_100))
                        ) {
                            Text(text = it.title)
                            Spacer(modifier = Modifier.weight(1f))
                            if (rangeSelection.selectionType == it) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(id = androidx.compose.ui.R.string.selected),
                                    tint = MaterialTheme.colors.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.size(dimensionResource(dimen.image_minor_50)))
                            }
                        }
                    }
                }
            }
        }
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
