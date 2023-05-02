package com.woocommerce.android.apifaker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.apifaker.models.ApiType.Custom
import com.woocommerce.android.apifaker.models.ApiType.WPApi
import com.woocommerce.android.apifaker.models.ApiType.WPCom
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.ui.Screen

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    onExit: () -> Unit
) {
    HomeScreen(
        endpoints = viewModel.endpoints.collectAsState().value,
        isEnabled = viewModel.isEnabled.collectAsState(initial = false).value,
        navController = navController,
        onRemoveRequest = viewModel::onRemoveRequest,
        onMockingToggleChanged = viewModel::onMockingToggleChanged,
        onExit = onExit
    )
}

@Composable
private fun HomeScreen(
    endpoints: List<MockedEndpoint>,
    isEnabled: Boolean,
    navController: NavController,
    onRemoveRequest: (Request) -> Unit = {},
    onMockingToggleChanged: (Boolean) -> Unit = {},
    onExit: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "ApiFaker") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onMockingToggleChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                    )
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (endpoints.isNotEmpty()) {
                LazyColumn {
                    items(endpoints, { endpoint -> endpoint.request.id }) { endpoint ->
                        EndpointItem(
                            endpoint,
                            onRemoveRequest = onRemoveRequest,
                            navController,
                            Modifier.padding(vertical = 8.dp)
                        )
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EndpointItem(
    endpoint: MockedEndpoint,
    onRemoveRequest: (Request) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberDismissState()

    if (dismissState.isDismissed(DismissDirection.EndToStart)) {
        onRemoveRequest(endpoint.request)
    }
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        dismissThresholds = {
            FractionalThreshold(0.3f)
        },
        modifier = modifier,
        background = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Red, MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        },
        dismissContent = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { navController.navigate(Screen.EndpointDetails.route(endpoint.request.id)) }),
                elevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = when (endpoint.request.type) {
                                WPApi -> "WordPress API"
                                WPCom -> "WordPress.com API"
                                is Custom -> "Host: ${endpoint.request.type.host}"
                            },
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold
                        )
                        val pathLine = endpoint.request.httpMethod?.let { "$it " }.orEmpty() +
                            endpoint.request.path
                        Text(
                            text = pathLine,
                            style = MaterialTheme.typography.body1
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 16.dp)
                    )
                    Text(
                        text = endpoint.response.statusCode.toString(),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    )
}

@Composable
@Preview
private fun HomeScreenPreview() {
    HomeScreen(
        endpoints = listOf(
            MockedEndpoint(
                Request(
                    type = WPApi,
                    httpMethod = null,
                    path = "/wc/v3/products",
                    body = ""
                ),
                Response(statusCode = 200, body = "")
            ),
            MockedEndpoint(
                Request(
                    type = WPCom,
                    httpMethod = HttpMethod.GET,
                    path = "/v1.1/me/sites",
                    body = ""
                ),
                Response(statusCode = 404, body = "")
            )
        ),
        isEnabled = true,
        navController = rememberNavController()
    )
}
