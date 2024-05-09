package com.woocommerce.android.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.pos.ui.theme.WCAndroidTheme
import com.woocommerce.android.util.AddressUtils

class PosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WCAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PosOneScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PosOneScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Screen #1 - ${AddressUtils.getCountryLabelByCountryCode("US")}",
        modifier = modifier
    )
}
