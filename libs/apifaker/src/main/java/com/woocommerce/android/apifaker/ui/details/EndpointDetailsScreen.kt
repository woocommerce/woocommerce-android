package com.woocommerce.android.apifaker.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.QueryParameter
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.ui.DropDownMenu
import kotlin.math.min

@Composable
internal fun EndpointDetailsScreen(
    viewModel: EndpointDetailsViewModel,
    navController: NavController
) {
    if (viewModel.state.isEndpointSaved) {
        navController.navigateUp()
    }

    EndpointDetailsScreen(
        state = viewModel.state,
        navController = navController,
        onSaveClicked = viewModel::onSaveClicked,
        onApiTypeChanged = viewModel::onApiTypeChanged,
        onRequestHttpMethodChanged = viewModel::onRequestHttpMethodChanged,
        onRequestPathChanged = viewModel::onRequestPathChanged,
        onQueryParameterAdded = viewModel::onQueryParameterAdded,
        onQueryParameterDeleted = viewModel::onQueryParameterDeleted,
        onRequestBodyChanged = viewModel::onRequestBodyChanged,
        onResponseStatusCodeChanged = viewModel::onResponseStatusCodeChanged,
        onResponseBodyChanged = viewModel::onResponseBodyChanged,
    )
}

@Composable
private fun EndpointDetailsScreen(
    state: EndpointDetailsViewModel.UiState,
    navController: NavController,
    onSaveClicked: () -> Unit = {},
    onApiTypeChanged: (ApiType) -> Unit = {},
    onRequestHttpMethodChanged: (HttpMethod?) -> Unit = {},
    onRequestPathChanged: (String) -> Unit = {},
    onQueryParameterAdded: (String, String) -> Unit = { _, _ -> },
    onQueryParameterDeleted: (QueryParameter) -> Unit = {},
    onRequestBodyChanged: (String) -> Unit = {},
    onResponseStatusCodeChanged: (Int) -> Unit = {},
    onResponseBodyChanged: (String) -> Unit = {},
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
                    TextButton(
                        onClick = onSaveClicked,
                        enabled = state.isEndpointValid,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onSurface)
                    ) {
                        Text(
                            text = "Save"
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            EndpointDefinitionSection(
                request = state.request,
                onApiTypeChanged = onApiTypeChanged,
                onHttpMethodChanged = onRequestHttpMethodChanged,
                onPathChanged = onRequestPathChanged,
                onQueryParameterAdded = onQueryParameterAdded,
                onQueryParameterDeleted = onQueryParameterDeleted,
                onBodyChanged = onRequestBodyChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Divider(
                Modifier.padding(horizontal = 8.dp),
                thickness = 2.dp
            )

            ResponseSection(
                response = state.response,
                onStatusCodeChanged = onResponseStatusCodeChanged,
                onBodyChanged = onResponseBodyChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun EndpointDefinitionSection(
    request: Request,
    onApiTypeChanged: (ApiType) -> Unit,
    onHttpMethodChanged: (HttpMethod?) -> Unit,
    onPathChanged: (String) -> Unit,
    onQueryParameterAdded: (String, String) -> Unit,
    onQueryParameterDeleted: (QueryParameter) -> Unit,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = "Endpoint Conditions",
            style = MaterialTheme.typography.h6
        )
        EndpointTypeField(
            apiType = request.type,
            onApiTypeChanged = onApiTypeChanged,
            modifier = Modifier.fillMaxWidth()
        )

        HttpMethodField(
            method = request.httpMethod,
            onHttpMethodChanged = onHttpMethodChanged,
            modifier = Modifier.fillMaxWidth()
        )

        PathField(
            path = request.path,
            apiType = request.type,
            onPathChanged = onPathChanged,
            modifier = Modifier.fillMaxWidth()
        )

        QueryParametersField(
            queryParameters = request.queryParameters,
            onQueryParameterAdded = onQueryParameterAdded,
            onQueryParameterDeleted = onQueryParameterDeleted,
            modifier = Modifier.fillMaxWidth()
        )

        RequestBodyField(
            body = request.body,
            onBodyChanged = onBodyChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResponseSection(
    response: Response,
    onStatusCodeChanged: (Int) -> Unit,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = "Response",
            style = MaterialTheme.typography.h6
        )
        StatusCodeField(
            statusCode = response.statusCode,
            onStatusCodeChanged = onStatusCodeChanged,
            modifier = Modifier.fillMaxWidth()
        )
        ResponseBodyField(
            body = response.body,
            onBodyChanged = onBodyChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EndpointTypeField(
    apiType: ApiType,
    onApiTypeChanged: (ApiType) -> Unit,
    modifier: Modifier = Modifier
) {
    fun ApiType.label() = when (this) {
        ApiType.WPApi -> "WordPress REST API"
        ApiType.WPCom -> "WordPress.com REST API"
        is ApiType.Custom -> "Custom"
    }

    DropDownMenu(
        label = "Type",
        currentValue = apiType,
        values = ApiType.defaultValues(),
        onValueChange = onApiTypeChanged,
        formatter = ApiType::label,
        modifier = modifier.fillMaxWidth()
    )
    if (apiType is ApiType.Custom) {
        OutlinedTextField(
            label = { Text(text = "Host (without scheme)") },
            value = apiType.host,
            onValueChange = { onApiTypeChanged(apiType.copy(host = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HttpMethodField(
    method: HttpMethod?,
    onHttpMethodChanged: (HttpMethod?) -> Unit,
    modifier: Modifier = Modifier
) {
    DropDownMenu(
        label = "HTTP Method",
        currentValue = method,
        values = listOf(null) + HttpMethod.values(),
        onValueChange = onHttpMethodChanged,
        formatter = { it?.name ?: "Any" },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PathField(
    path: String,
    apiType: ApiType,
    onPathChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        OutlinedTextField(
            label = { Text(text = "Path") },
            value = path,
            onValueChange = onPathChanged,
            modifier = Modifier.fillMaxWidth()
        )
        val prefix = when (apiType) {
            ApiType.WPApi -> "/wp-json"
            ApiType.WPCom -> "/rest"
            is ApiType.Custom -> "host"
        }
        val caption = buildAnnotatedString {
            append("Enter the path after the")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(" $prefix ")
            }
            append("part, without the query arguments")
            append("\n")
            append("Use")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(" % ")
            }
            append("as a wildcard for zero or more characters")
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun QueryParametersField(
    queryParameters: List<QueryParameter>,
    onQueryParameterAdded: (String, String) -> Unit,
    onQueryParameterDeleted: (QueryParameter) -> Unit,
    modifier: Modifier = Modifier
) {
    @Composable
    fun AddDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
        var name by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        Dialog(onDismissRequest = onDismiss) {
            Column(
                Modifier
                    .background(MaterialTheme.colors.surface, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text(text = "Add Query Parameter (unencoded)")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    label = { Text(text = "Name") },
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    label = { Text(text = "Value") },
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "The value field accepts the wildcard % for zero or more characters",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onDismiss) {
                        Text(text = "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAdd(name, value) }) {
                        Text(text = "Add")
                    }
                }

            }
        }
    }

    var isAddDialogShown by remember { mutableStateOf(false) }

    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Query Parameters",
                style = MaterialTheme.typography.subtitle1
            )
            IconButton(
                onClick = { isAddDialogShown = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add query parameter"
                )
            }
        }
        Text(
            text = "A request is matched only if it contains all the parameters listed here",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (queryParameters.isNotEmpty()) {
            queryParameters.forEach { queryParameter ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "${queryParameter.name} = ${queryParameter.value}",
                        style = MaterialTheme.typography.caption
                    )
                    IconButton(
                        onClick = { onQueryParameterDeleted(queryParameter) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete query parameter"
                        )
                    }
                }
                Divider()
            }
        }
    }

    if (isAddDialogShown) {
        AddDialog(onAdd = { name, value ->
            onQueryParameterAdded(name, value)
            isAddDialogShown = false
        },
            onDismiss = { isAddDialogShown = false }
        )
    }
}

@Composable
private fun RequestBodyField(
    body: String?,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        OutlinedTextField(
            label = { Text(text = "Body") },
            value = body.orEmpty(),
            placeholder = { Text(text = "An empty value will match everything") },
            textStyle = if (body != null) LocalTextStyle.current
            else LocalTextStyle.current.copy(color = Color.Gray),
            onValueChange = onBodyChanged,
            modifier = Modifier
                .fillMaxWidth()
        )
        val caption = buildAnnotatedString {
            append("Use")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(" % ")
            }
            append("as a wildcard for zero or more characters")
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun StatusCodeField(
    statusCode: Int,
    onStatusCodeChanged: (Int) -> Unit,
    modifier: Modifier
) {
    @Composable
    fun StatusCodeChip(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Chip(
            onClick = onClick,
            border = ChipDefaults.outlinedBorder,
            colors = ChipDefaults.outlinedChipColors(
                backgroundColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                contentColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
            )
        ) {
            Text(text = text)
        }
    }

    @Composable
    fun CustomStatusCodeDialog(
        statusCode: Int,
        onDismiss: () -> Unit,
        onStatusCodeChanged: (Int) -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.colors.surface, shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                var fieldValue by remember {
                    mutableStateOf(statusCode.toString())
                }
                OutlinedTextField(
                    label = { Text(text = "Custom status code") },
                    value = fieldValue,
                    onValueChange = {
                        fieldValue = it.substring(0, min(it.length, 3))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        onStatusCodeChanged(fieldValue.toInt())
                    },
                    enabled = fieldValue.length == 3 && fieldValue.toIntOrNull() != null,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "OK")
                }
            }
        }
    }

    val defaultStatusCodes = remember {
        arrayOf(200, 403, 404, 500)
    }
    val isUsingACustomValue = remember(statusCode) {
        !defaultStatusCodes.contains(statusCode)
    }

    var showCustomStatusCodeDialog by remember {
        mutableStateOf(false)
    }

    Column(modifier) {
        Text(
            text = "Status Code",
            style = MaterialTheme.typography.subtitle1
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            defaultStatusCodes.forEach { code ->
                StatusCodeChip(
                    text = code.toString(),
                    selected = statusCode == code,
                    onClick = { onStatusCodeChanged(code) }
                )
            }

            StatusCodeChip(
                text = "Custom${if (isUsingACustomValue) " ($statusCode) " else ""}",
                selected = isUsingACustomValue,
                onClick = { showCustomStatusCodeDialog = true }
            )
        }
        if (showCustomStatusCodeDialog) {
            CustomStatusCodeDialog(
                statusCode = statusCode,
                onDismiss = { showCustomStatusCodeDialog = false },
                onStatusCodeChanged = {
                    showCustomStatusCodeDialog = false
                    onStatusCodeChanged(it)
                }
            )
        }
    }
}

@Composable
private fun ResponseBodyField(
    body: String?,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        label = { Text(text = "Body") },
        value = body.orEmpty(),
        onValueChange = onBodyChanged,
        modifier = modifier
            .defaultMinSize(minHeight = TextFieldDefaults.MinHeight * 2)
    )
}

@Composable
@Preview
private fun EndpointDetailsScreenPreview() {
    Surface(color = MaterialTheme.colors.background) {
        EndpointDetailsScreen(
            state = EndpointDetailsViewModel.UiState(
                request = Request(
                    type = ApiType.Custom("https://example.com"),
                    httpMethod = HttpMethod.GET,
                    queryParameters = listOf(
                        QueryParameter("name", "value"),
                        QueryParameter("name2", "value2")
                    ),
                    path = "/wc/v3/products",
                    body = null
                ),
                response = Response(
                    statusCode = 300,
                    body = ""
                )
            ),
            navController = rememberNavController()
        )
    }
}
