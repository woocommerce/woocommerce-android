package com.woocommerce.android.ui.common.texteditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier

@Composable
fun SimpleTextEditorScreen(viewModel: SimpleTextEditorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    SimpleTextEditorScreen(viewState?.text, viewModel::onTextChanged)
}

@Composable
fun SimpleTextEditorScreen(text: String?, onTextChanged: (String) -> Unit) {
    TextField(value = text.orEmpty(), onValueChange = onTextChanged, modifier = Modifier.fillMaxSize())
}
