package com.woocommerce.android.ui.mystore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography

@Composable
fun MyStoreScreen(viewModel: MyStoreViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    MyStoreScreen(
        currentSiteName = viewState?.currentSiteName.orEmpty()
    )
}

@Composable
fun MyStoreScreen(
    currentSiteName: String?
) {
    WooTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 16.dp
                    )
            ) {
                Text(
                    text = currentSiteName ?: "No site selected",
                    textAlign = TextAlign.Center,
                    style = WooTypography.caption1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                Text(text = "Today")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StoreDataItem(
                        title = "Revenue",
                        value = "$4,321.90",
                        modifier = Modifier.weight(1f)
                    )
                    StoreDataItem(
                        title = "Visitors",
                        value = "15",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StoreDataItem(
                        title = "Orders",
                        value = "5",
                        modifier = Modifier.weight(1f)
                    )
                    StoreDataItem(
                        title = "Conversion",
                        value = "25%",
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("View all orders")
                }
            }
        }
    }
}

@Composable
fun StoreDataItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = WooTypography.caption1
        )
        Text(text = value)
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MyStoreScreen(
        currentSiteName = "My Store"
    )
}
