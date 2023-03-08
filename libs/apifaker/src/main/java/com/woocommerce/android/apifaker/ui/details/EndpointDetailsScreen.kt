package com.woocommerce.android.apifaker.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
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
import com.woocommerce.android.apifaker.models.Endpoint
import com.woocommerce.android.apifaker.models.EndpointType
import com.woocommerce.android.apifaker.models.FakeResponse
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
        onEndpointTypeChanged = viewModel::onEndpointTypeChanged,
        onRequestPathChanged = viewModel::onRequestPathChanged,
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
    onEndpointTypeChanged: (EndpointType) -> Unit = {},
    onRequestPathChanged: (String) -> Unit = {},
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
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onPrimary)
                    ) {
                        Text(
                            text = "Save"
                        )
                    }
                }
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
                endpoint = state.endpoint,
                onEndpointTypeChanged = onEndpointTypeChanged,
                onPathChanged = onRequestPathChanged,
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
    endpoint: Endpoint,
    onEndpointTypeChanged: (EndpointType) -> Unit,
    onPathChanged: (String) -> Unit,
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
            endpointType = endpoint.type,
            onEndpointTypeChanged = onEndpointTypeChanged,
            modifier = Modifier.fillMaxWidth()
        )

        PathField(
            path = endpoint.path,
            endpointType = endpoint.type,
            onPathChanged = onPathChanged,
            modifier = Modifier.fillMaxWidth()
        )

        RequestBodyField(
            body = endpoint.body,
            onBodyChanged = onBodyChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResponseSection(
    response: FakeResponse,
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
    endpointType: EndpointType,
    onEndpointTypeChanged: (EndpointType) -> Unit,
    modifier: Modifier = Modifier
) {
    fun EndpointType.label() = when (this) {
        EndpointType.WPApi -> "WordPress REST API"
        EndpointType.WPCom -> "WordPress.com REST API"
        is EndpointType.Custom -> "Custom"
    }

    DropDownMenu(
        label = "Type",
        currentValue = endpointType,
        values = EndpointType.defaultValues(),
        onValueChange = onEndpointTypeChanged,
        formatter = EndpointType::label,
        modifier = modifier.fillMaxWidth()
    )
    if (endpointType is EndpointType.Custom) {
        TextField(
            label = { Text(text = "Host (without scheme)") },
            value = endpointType.host,
            onValueChange = { onEndpointTypeChanged(endpointType.copy(host = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PathField(
    path: String,
    endpointType: EndpointType,
    onPathChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        TextField(
            label = { Text(text = "Path") },
            value = path,
            onValueChange = onPathChanged,
            modifier = Modifier.fillMaxWidth()
        )
        val prefix = when (endpointType) {
            EndpointType.WPApi -> "/wp-json"
            EndpointType.WPCom -> "/rest"
            is EndpointType.Custom -> "host"
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
            append("as a wildcard for one or more characters")
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.caption
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
        TextField(
            label = { Text(text = "Body") },
            value = body.orEmpty(),
            placeholder = { Text(text = "An empty value will match everything") },
            textStyle = if (body != null) LocalTextStyle.current
            else LocalTextStyle.current.copy(color = Color.Gray),
            onValueChange = onBodyChanged,
            modifier = Modifier.fillMaxWidth()
        )
        val caption = buildAnnotatedString {
            append("Use")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(" % ")
            }
            append("as a wildcard for one or more characters")
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.caption
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
                TextField(
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
    TextField(
        label = { Text(text = "Body") },
        value = body.orEmpty(),
        onValueChange = onBodyChanged,
        modifier = modifier
    )
}

@Composable
@Preview
private fun EndpointDetailsScreenPreview() {
    Surface(color = MaterialTheme.colors.background) {
        EndpointDetailsScreen(
            state = EndpointDetailsViewModel.UiState(
                endpoint = Endpoint(
                    type = EndpointType.Custom("https://example.com"),
                    path = "/wc/v3/products",
                    body = null
                ),
                response = FakeResponse(
                    statusCode = 300,
                    body = ""
                )
            ),
            navController = rememberNavController()
        )
    }
}
