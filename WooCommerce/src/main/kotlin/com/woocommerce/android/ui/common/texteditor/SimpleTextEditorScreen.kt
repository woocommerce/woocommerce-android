package com.woocommerce.android.ui.common.texteditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun SimpleTextEditorScreen(viewModel: SimpleTextEditorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    viewState?.let { state ->
        SimpleTextEditorScreen(
            screenTitle = state.screenTitle,
            text = state.text,
            hint = state.hint,
            strategy = state.strategy,
            viewModel::onTextChanged,
            viewModel::onDonePressed,
            viewModel::onBackPressed,
        )
    }
}

@Composable
fun SimpleTextEditorScreen(
    screenTitle: String,
    text: String,
    hint: String,
    strategy: SimpleTextEditorStrategy,
    onTextChanged: (String) -> Unit,
    onDonePressed: () -> Unit,
    onBackPressed: () -> Unit,
) {
    BackHandler {
        onBackPressed()
    }
    Scaffold(
        topBar = {
            Toolbar(
                title = screenTitle,
                onNavigationButtonClick = onBackPressed,
                navigationIcon = ImageVector.vectorResource(
                    when (strategy) {
                        SimpleTextEditorStrategy.SEND_RESULT_ON_CONFIRMATION -> R.drawable.ic_close_24dp
                        SimpleTextEditorStrategy.SEND_RESULT_ON_NAVIGATE_BACK -> R.drawable.ic_back_24dp
                    }
                ),
                actions = {
                    if (strategy == SimpleTextEditorStrategy.SEND_RESULT_ON_CONFIRMATION) {
                        TextButton(onClick = onDonePressed) {
                            Text(text = stringResource(id = R.string.done))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        TextField(
            value = text,
            onValueChange = onTextChanged,
            placeholder = {
                Text(hint)
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}
