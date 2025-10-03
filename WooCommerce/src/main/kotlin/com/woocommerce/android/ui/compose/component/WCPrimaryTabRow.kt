package com.woocommerce.android.ui.compose.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.woocommerce.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> WCPrimaryTabRow(
    tabs: List<T>,
    selectedTab: T,
    tabName: @Composable (T) -> String,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTabIndex = tabs.indexOf(selectedTab)

    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = colorResource(R.color.color_toolbar),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(selectedTabIndex, matchContentSize = false),
                width = Dp.Unspecified
            )
        },
        modifier = modifier,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tabName(tab),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (selectedTab == tab) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            )
        }
    }
}
