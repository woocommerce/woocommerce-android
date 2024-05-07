package com.woocommerce.android.ui.mystore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.presentation.theme.WooColors
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography

@Composable
fun MyStoreScreen(viewModel: MyStoreViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    MyStoreScreen(
        isLoading = viewState?.isLoading ?: false,
        currentSiteName = viewState?.currentSiteName.orEmpty(),
        totalRevenue = viewState?.revenueTotal?.toString().orEmpty(),
        ordersCount = viewState?.ordersCount?.toString().orEmpty(),
        visitorsCount = viewState?.visitorsCount?.toString().orEmpty(),
        conversionRate = viewState?.conversionRate.orEmpty()
    )
}

@Composable
fun MyStoreScreen(
    isLoading: Boolean,
    currentSiteName: String,
    totalRevenue: String,
    ordersCount: String,
    visitorsCount: String,
    conversionRate: String
) {
    WooTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val brush = Brush.verticalGradient(listOf(
                WooColors.purple_surface, Color.Black))
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    drawRect(brush)
                }
            )
            TimeText()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = currentSiteName,
                    textAlign = TextAlign.Center,
                    style = WooTypography.body1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                if (isLoading) {
                    LoadingScreen()
                } else {
                    MyStoreView(
                        totalRevenue,
                        visitorsCount,
                        ordersCount,
                        conversionRate
                    )
                }
            }
        }
    }
}

@Composable
private fun MyStoreView(
    totalRevenue: String,
    visitorsCount: String,
    ordersCount: String,
    conversionRate: String
) {
    Box {
        Column {
            Text(
                text = "Revenue",
                textAlign = TextAlign.Center,
                style = WooTypography.body2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Text(
                text = totalRevenue,
                textAlign = TextAlign.Center,
                style = WooTypography.display3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                StoreDataItem(
                    icon = Icons.Filled.Description,
                    value = visitorsCount,
                )
                Spacer(modifier = Modifier.size(24.dp))
                StoreDataItem(
                    icon = Icons.Filled.Group,
                    value = ordersCount,
                )
                Spacer(modifier = Modifier.size(24.dp))
                StoreDataItem(
                    icon = Icons.Filled.Timeline,
                    value = conversionRate,
                )
            }
        }
        Text(
            text = "Today • As of 02:19",
            style = WooTypography.caption2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }

}

@Composable
fun StoreDataItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String
) {
    Column(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(text = value)
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MyStoreScreen(
        isLoading = false,
        currentSiteName = "My Store",
        totalRevenue = "$5,321.90",
        ordersCount = "12",
        visitorsCount = "123",
        conversionRate = "10%"
    )
}
