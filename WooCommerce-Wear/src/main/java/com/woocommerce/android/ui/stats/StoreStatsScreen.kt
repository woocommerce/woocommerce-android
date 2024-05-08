package com.woocommerce.android.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.presentation.theme.WooColors
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography

@Composable
fun StoreStatsScreen(viewModel: StoreStatsViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    StoreStatsScreen(
        isLoading = viewState?.isLoading ?: false,
        currentSiteName = viewState?.currentSiteName.orEmpty(),
        totalRevenue = viewState?.revenueTotal.orEmpty(),
        ordersCount = viewState?.ordersCount?.toString().orEmpty(),
        visitorsCount = viewState?.visitorsCount?.toString().orEmpty(),
        conversionRate = viewState?.conversionRate.orEmpty(),
        timestamp = viewState?.timestamp.orEmpty()
    )
}

@Composable
fun StoreStatsScreen(
    isLoading: Boolean,
    currentSiteName: String,
    totalRevenue: String,
    ordersCount: String,
    visitorsCount: String,
    conversionRate: String,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    WooTheme {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val brush = Brush.verticalGradient(
                listOf(
                    WooColors.woo_purple_surface,
                    Color.Black
                )
            )
            Canvas(
                modifier = modifier.fillMaxSize(),
                onDraw = {
                    drawRect(brush)
                }
            )
            TimeText()
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = currentSiteName,
                    textAlign = TextAlign.Center,
                    style = WooTypography.body1,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                if (isLoading) {
                    LoadingScreen()
                } else {
                    StatsContentScreen(
                        modifier,
                        totalRevenue,
                        visitorsCount,
                        ordersCount,
                        conversionRate,
                        timestamp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsContentScreen(
    modifier: Modifier,
    totalRevenue: String,
    visitorsCount: String,
    ordersCount: String,
    conversionRate: String,
    timestamp: String
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Text(
                text = stringResource(id = R.string.stats_screen_revenue_title),
                textAlign = TextAlign.Center,
                color = WooColors.woo_purple_5,
                style = WooTypography.body2,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Text(
                text = totalRevenue,
                textAlign = TextAlign.Center,
                style = WooTypography.display3,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                IconStats(
                    icon = Icons.Filled.Description,
                    value = ordersCount,
                )
                IconStats(
                    icon = Icons.Filled.Group,
                    value = visitorsCount,
                )
                IconStats(
                    icon = Icons.Filled.Timeline,
                    value = conversionRate,
                )
            }
        }

        Text(
            text = stringResource(id = R.string.stats_screen_time_description, timestamp),
            style = WooTypography.caption2,
            textAlign = TextAlign.Center,
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun IconStats(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String
) {
    Column(
        modifier = modifier
            .size(50.dp)
    ) {
        Icon(
            imageVector = icon,
            tint = WooColors.woo_purple_10,
            contentDescription = null,
            modifier = modifier
                .align(Alignment.CenterHorizontally)
                .size(18.dp)
        )
        Text(
            text = value,
            modifier = modifier
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun DefaultPreview() {
    StoreStatsScreen(
        isLoading = false,
        currentSiteName = "My Store",
        totalRevenue = "$5,321.90",
        ordersCount = "5",
        visitorsCount = "12",
        conversionRate = "100%",
        timestamp = "02:19"
    )
}