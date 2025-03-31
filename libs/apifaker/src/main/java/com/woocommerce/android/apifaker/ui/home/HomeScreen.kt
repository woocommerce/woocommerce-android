package com.woocommerce.android.apifaker.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.DismissDirection
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.apifaker.ExportImportDestination
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.ui.Screen

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    onExit: () -> Unit
) {
    HomeScreen(
        endpoints = viewModel.endpoints.collectAsStateWithLifecycle().value,
        isEnabled = viewModel.isEnabled.collectAsState(initial = false).value,
        navController = navController,
        snackbarHostState = snackbarHostState,
        onRemoveRequest = viewModel::onRemoveRequest,
        onMockingToggleChanged = viewModel::onMockingToggleChanged,
        onExportEndpoints = viewModel::onExportEndpoints,
        onImportEndpoints = viewModel::onImportEndpoints,
        onExit = onExit
    )
}

@Composable
private fun HomeScreen(
    endpoints: List<MockedEndpoint>,
    isEnabled: Boolean,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    onRemoveRequest: (Request) -> Unit,
    onMockingToggleChanged: (Boolean) -> Unit,
    onExportEndpoints: (ExportImportDestination) -> Unit,
    onImportEndpoints: (ExportImportDestination) -> Unit,
    onExit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "API Faker") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Switch(checked = isEnabled, onCheckedChange = onMockingToggleChanged)

                    TopMenu(
                        hasEndpoints = endpoints.isNotEmpty(),
                        onExportEndpoints = onExportEndpoints,
                        onImportEndpoints = onImportEndpoints
                    )
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
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

@Composable
private fun TopMenu(
    hasEndpoints: Boolean,
    onExportEndpoints: (ExportImportDestination) -> Unit,
    onImportEndpoints: (ExportImportDestination) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { expanded = !expanded }) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "More"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        if (hasEndpoints) {
            ExportMenuButton(
                onDismiss = { expanded = false },
                onExportEndpoints = {
                    expanded = false
                    onExportEndpoints(it)
                }
            )
        }
        ImportMenuButton(
            onDismiss = { expanded = false },
            onImportEndpoints = {
                expanded = false
                onImportEndpoints(it)
            }
        )
    }
}

@Composable
fun ExportMenuButton(
    onDismiss: () -> Unit,
    onExportEndpoints: (ExportImportDestination) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let { onExportEndpoints(ExportImportDestination.File(it)) }
    }

    var showDialog by rememberSaveable { mutableStateOf(false) }

    DropdownMenuItem(onClick = { showDialog = true }) {
        Text("Export")
    }

    if (showDialog) {
        Dialog({
            showDialog = false
            onDismiss()
        }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(MaterialTheme.colors.surface, MaterialTheme.shapes.medium)
                    .padding(16.dp)
                    .defaultMinSize(minWidth = 200.dp)
            ) {
                Text("Export to:", Modifier.align(Alignment.Start))
                TextButton(onClick = {
                    showDialog = false
                    launcher.launch("endpoints.json")
                }) {
                    Text("File")
                }
                TextButton(onClick = {
                    showDialog = false
                    onExportEndpoints(ExportImportDestination.Clipboard)
                }) {
                    Text("Clipboard")
                }
            }
        }
    }
}

@Composable
private fun ImportMenuButton(
    onDismiss: () -> Unit,
    onImportEndpoints: (ExportImportDestination) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { onImportEndpoints(ExportImportDestination.File(it)) }
    }

    var showDialog by rememberSaveable { mutableStateOf(false) }

    DropdownMenuItem(onClick = { showDialog = true }) {
        Text("Import")
    }

    if (showDialog) {
        Dialog({
            showDialog = false
            onDismiss()
        }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(MaterialTheme.colors.surface, MaterialTheme.shapes.medium)
                    .padding(16.dp)
                    .defaultMinSize(minWidth = 200.dp)
            ) {
                Text("Import from:", Modifier.align(Alignment.Start))
                TextButton(onClick = {
                    showDialog = false
                    launcher.launch(arrayOf("application/json"))
                }) {
                    Text("File")
                }
                TextButton(onClick = {
                    showDialog = false
                    onImportEndpoints(ExportImportDestination.Clipboard)
                }) {
                    Text("Clipboard")
                }
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
            @Suppress("DEPRECATION")
            androidx.compose.material.FractionalThreshold(0.3f)
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (endpoint.request.type) {
                                ApiType.WPApi -> "WordPress API"
                                ApiType.WPCom -> "WordPress.com API"
                                is ApiType.Custom -> "Host: ${endpoint.request.type.host}"
                            },
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold
                        )
                        val pathLine = endpoint.request.httpMethod?.let { "$it " }.orEmpty() + endpoint.request.path
                        Text(
                            text = pathLine,
                            style = MaterialTheme.typography.body1
                        )
                    }
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
                    type = ApiType.WPApi,
                    httpMethod = HttpMethod.GET,
                    path = "/wc/v3/products",
                    body = ""
                ),
                Response(statusCode = 200, body = "")
            ),
            MockedEndpoint(
                Request(
                    type = ApiType.WPCom,
                    httpMethod = HttpMethod.GET,
                    path = "/v1.1/me/sites",
                    body = ""
                ),
                Response(statusCode = 404, body = "")
            )
        ),
        isEnabled = true,
        snackbarHostState = remember { SnackbarHostState() },
        navController = rememberNavController(),
        onRemoveRequest = {},
        onMockingToggleChanged = {},
        onExportEndpoints = {},
        onImportEndpoints = {},
        onExit = {}
    )
}
