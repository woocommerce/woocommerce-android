package com.woocommerce.android.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.pos.ui.Routing
import com.woocommerce.android.util.AddressUtils

class PosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenKey = intent.getStringExtra("screen_key") ?: "default"
        val data = intent.getStringExtra("id") ?: ""
        val startDestination = when (screenKey) {
            "pos_screen_two" -> {
                Routing.PosScreenTwo.route.replace("{id}", data)
            }

            else -> Routing.PosScreenOne.route
        }

        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val navController = rememberNavController()
                NavHost(navController, startDestination = startDestination) {
                    composable(Routing.PosScreenOne.route) {
                        PosScreenOne(modifier = Modifier.padding(innerPadding)) {
                            navController.navigate(Routing.PosScreenTwo.route.replace("{id}", "100"))
                        }
                    }
                    composable(Routing.PosScreenTwo.route) { backStackEntry ->
                        PosScreenTwo(
                            backStackEntry.arguments?.getString("id") ?: data,
                            onClick = { finish() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PosScreenOne(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Screen #1",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Uses util from core - ${AddressUtils.getCountryLabelByCountryCode("US")}",
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Text("Go to screen #2")
        }
    }
}

@Composable
fun PosScreenTwo(data: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Screen #2",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Received data: $data",
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onClick() }
        ) {
            Text("Go Back to the main app")
        }
    }
}
