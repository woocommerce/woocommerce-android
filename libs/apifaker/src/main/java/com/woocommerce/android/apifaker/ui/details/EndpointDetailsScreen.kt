package com.woocommerce.android.apifaker.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.apifaker.models.Endpoint
import com.woocommerce.android.apifaker.models.EndpointType
import com.woocommerce.android.apifaker.models.EndpointWithResponse
import com.woocommerce.android.apifaker.models.FakeResponse
import com.woocommerce.android.apifaker.ui.DropDownMenu

@Composable
internal fun EndpointDetailsScreen(
    viewModel: EndpointDetailsViewModel,
    navController: NavController
) {
    EndpointDetailsScreen(
        state = viewModel.state,
        navController = navController,
        onEndpointTypeChanged = viewModel::onEndpointTypeChanged
    )
}

@Composable
private fun EndpointDetailsScreen(
    state: EndpointWithResponse,
    navController: NavController,
    onEndpointTypeChanged: (EndpointType) -> Unit = {},
    onPathChanged: (String) -> Unit = {},
    onBodyChanged: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Endpoint Definition") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { /*TODO*/ }) {
                        Text(text = "Save")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EndpointDefinitionSection(
                endpoint = state.endpoint,
                onEndpointTypeChanged = onEndpointTypeChanged,
                onPathChanged = onPathChanged,
                onBodyChanged = onBodyChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EndpointDefinitionSection(
    endpoint: Endpoint,
    onEndpointTypeChanged: (EndpointType) -> Unit,
    onPathChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        DropDownMenu(
            label = "Type",
            currentValue = endpoint.type,
            values = EndpointType.defaultValues(),
            onValueChange = onEndpointTypeChanged,
            formatter = EndpointType::label,
            modifier = Modifier.fillMaxWidth()
        )
        if (endpoint.type is EndpointType.Custom) {
            TextField(
                label = { Text(text = "Host (without scheme)") },
                value = endpoint.type.host,
                onValueChange = { onEndpointTypeChanged(endpoint.type.copy(host = it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val EndpointType.label
    get() = when (this) {
        EndpointType.WPApi -> "WordPress REST API"
        EndpointType.WPCom -> "WordPress.com REST API"
        is EndpointType.Custom -> "Custom"
    }

@Composable
@Preview
private fun EndpointDetailsScreenPreview() {
    Surface(color = MaterialTheme.colors.background) {
        EndpointDetailsScreen(
            state = EndpointWithResponse(
                endpoint = Endpoint(
                    type = EndpointType.Custom("https://example.com"),
                    path = "/wc/v3/products",
                    body = "%"
                ),
                response = FakeResponse(
                    statusCode = 200,
                    body = ""
                )
            ),
            navController = rememberNavController()
        )
    }
}
