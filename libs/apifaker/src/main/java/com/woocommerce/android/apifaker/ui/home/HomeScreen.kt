package com.woocommerce.android.apifaker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.ApiType.Custom
import com.woocommerce.android.apifaker.models.ApiType.WPApi
import com.woocommerce.android.apifaker.models.ApiType.WPCom
import com.woocommerce.android.apifaker.ui.Screen

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController
) {
    HomeScreen(
        requests = viewModel.endpoints.collectAsState().value,
        isEnabled = viewModel.isEnabled.collectAsState(initial = false).value,
        onMockingToggleChanged = viewModel::onMockingToggleChanged,
        navController = navController
    )
}

@Composable
private fun HomeScreen(
    requests: List<Request>,
    isEnabled: Boolean,
    onMockingToggleChanged: (Boolean) -> Unit = {},
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "API Faker") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Switch(checked = isEnabled, onCheckedChange = onMockingToggleChanged)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (requests.isNotEmpty()) {
                LazyColumn {
                    items(requests) { endpoint ->
                        EndpointItem(endpoint, navController, Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {
                Text(text = "Start by adding some endpoints")
            }

            FloatingActionButton(
                onClick = { navController.navigate(Screen.EndpointDetails.routeForCreation()) },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add endpoint")
            }
        }
    }
}

@Composable
private fun EndpointItem(
    request: Request,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { navController.navigate(Screen.EndpointDetails.route(request.id)) }),
        elevation = 4.dp
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = when (request.type) {
                    WPApi -> "WordPress API"
                    WPCom -> "WordPress.com API"
                    is Custom -> "Host: ${request.type.host}"
                },
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = request.path,
                style = MaterialTheme.typography.subtitle2
            )
        }
    }
}

@Composable
@Preview
private fun HomeScreenPreview() {
    HomeScreen(
        requests = listOf(
            Request(
                type = WPApi,
                path = "/wc/v3/products",
                body = ""
            ),
            Request(
                type = WPCom,
                path = "/v1.1/me/sites",
                body = ""
            ),
        ),
        isEnabled = true,
        navController = rememberNavController()
    )
}
