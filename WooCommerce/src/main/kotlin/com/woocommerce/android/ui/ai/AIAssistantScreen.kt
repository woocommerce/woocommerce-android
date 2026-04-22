package com.woocommerce.android.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.ai.AIAssistantViewModel.AIAssistantUiState
import com.woocommerce.android.ui.ai.AIAssistantViewModel.UiChatMessage
import com.woocommerce.android.ui.ai.components.ChatInputBar
import com.woocommerce.android.ui.ai.components.ChatMessageItem
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun AIAssistantScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AIAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AIAssistantScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::onSendMessage,
        onDismissError = viewModel::onDismissError,
        onRetry = viewModel::onRetry,
        onOrderClicked = viewModel::onOrderClicked,
        onProductClicked = viewModel::onProductClicked,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    uiState: AIAssistantUiState,
    onBackClick: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit,
    onOrderClicked: (Long) -> Unit = {},
    onProductClicked: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Toolbar(
                title = {
                    Column {
                        Text("AI Assistant")
                        if (uiState.mcpConnected) {
                            Text(
                                text = "${uiState.availableTools} tools available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                onNavigationButtonClick = onBackClick
            )
        },
        bottomBar = {
            Column {
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(8.dp),
                        action = {
                            TextButton(onClick = onRetry) {
                                Text("Retry")
                            }
                        },
                        dismissAction = {
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }

                ChatInputBar(
                    inputText = uiState.inputText,
                    onInputChanged = onInputChanged,
                    onSendClick = onSendMessage,
                    isEnabled = uiState.mcpConnected && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isConnecting -> {
                    ConnectionStatus(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.messages.isEmpty() -> {
                    EmptyState(
                        mcpConnected = uiState.mcpConnected,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    MessageList(
                        messages = uiState.messages,
                        isLoading = uiState.isLoading,
                        onOrderClicked = onOrderClicked,
                        onProductClicked = onProductClicked,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatus(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Connecting to store...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EmptyState(
    mcpConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (mcpConnected) {
                "Ask me anything about your store"
            } else {
                "Not connected to store"
            },
            style = MaterialTheme.typography.titleMedium
        )
        if (mcpConnected) {
            Text(
                text = "Try: \"Show me my recent orders\" or \"Create a 20% off coupon\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<UiChatMessage>,
    isLoading: Boolean,
    onOrderClicked: (Long) -> Unit,
    onProductClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            ChatMessageItem(
                message = message,
                onOrderClicked = onOrderClicked,
                onProductClicked = onProductClicked
            )
        }

        if (isLoading && messages.none { it.isStatus }) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
