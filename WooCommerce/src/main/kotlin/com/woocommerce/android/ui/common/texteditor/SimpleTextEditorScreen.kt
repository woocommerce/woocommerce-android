package com.woocommerce.android.ui.common.texteditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun SimpleTextEditorScreen(viewModel: SimpleTextEditorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    SimpleTextEditorScreen(
        screenTitle = viewState?.screenTitle,
        text = viewState?.text,
        hint = viewState?.hint,
        strategy = viewState?.strategy,
        viewModel::onTextChanged,
        viewModel::onDonePressed,
        viewModel::onBackPressed,
    )
}

@Composable
fun SimpleTextEditorScreen(
    screenTitle: String?,
    text: String?,
    hint: String?,
    strategy: SimpleTextEditorStrategy?,
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
                title = screenTitle.orEmpty(),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = when (strategy) {
                    SimpleTextEditorStrategy.SEND_RESULT_ON_CONFIRMATION -> Icons.Default.Clear
                    SimpleTextEditorStrategy.SEND_RESULT_ON_NAVIGATE_BACK, null -> Icons.AutoMirrored.Filled.ArrowBack
                },
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
            value = text.orEmpty(),
            onValueChange = onTextChanged,
            placeholder = {
                Text(hint.orEmpty())
            },
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}
