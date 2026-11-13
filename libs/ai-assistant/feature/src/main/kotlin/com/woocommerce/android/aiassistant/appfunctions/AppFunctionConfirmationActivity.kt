package com.woocommerce.android.aiassistant.appfunctions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class AppFunctionConfirmationActivity : ComponentActivity() {
    private val viewModel: AppFunctionConfirmationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppFunctionsTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                AppFunctionConfirmationDialog(state = state, onConfirm = viewModel::onConfirm, onDismiss = ::finish)
            }
        }
    }
}
