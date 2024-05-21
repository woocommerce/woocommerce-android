package com.woocommerce.android.ui.woopos.root

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WooPosRootHost()
            }
        }
    }
}
