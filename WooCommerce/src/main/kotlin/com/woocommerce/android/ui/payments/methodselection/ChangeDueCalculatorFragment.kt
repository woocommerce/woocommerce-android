package com.woocommerce.android.ui.payments.methodselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeDueCalculatorFragment : DialogFragment() {

    private val viewModel: ChangeDueCalculatorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ChangeDueCalculatorScreen()
            }
        }
    }


    @Composable
    fun ChangeDueCalculatorScreen() {
        val uiState by viewModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display dynamic content based on UI state
            when (uiState) {
                is ChangeDueCalculatorViewModel.UiState.Loading -> {
                    Text(text = stringResource(R.string.loading), style = MaterialTheme.typography.h5)
                }

                is ChangeDueCalculatorViewModel.UiState.Success -> {
                    val state = uiState as ChangeDueCalculatorViewModel.UiState.Success
                    Text(
                        text = stringResource(R.string.cash_payments_take_payment_title, state.amountDue),
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                }

                is ChangeDueCalculatorViewModel.UiState.Error -> {
                    Text(text = stringResource(R.string.error_generic), style = MaterialTheme.typography.h5)
                }
            }
        }
    }
}





