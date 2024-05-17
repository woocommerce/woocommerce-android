package com.woocommerce.android.ui.payments.methodselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeDueCalculatorFragment : BaseFragment() {

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
        val uiState = viewModel.uiState.collectAsState().value

        Scaffold(
            topBar = {
                TopAppBar(
                    backgroundColor = colorResource(id = R.color.color_toolbar),
                    navigationIcon = {
                        IconButton(onClick = { findNavController().navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(
                                    R.string.back
                                )
                            )
                        }
                    },
                    title = {
                        val titleText = when (uiState) {
                            is ChangeDueCalculatorViewModel.UiState.Success -> {
                                stringResource(
                                    R.string.cash_payments_take_payment_title,
                                    uiState.amountDue
                                )
                            }
                            else -> stringResource(id = R.string.cash_payments_take_payment_title)
                        }
                        Text(text = titleText, modifier = Modifier.padding(start = 16.dp))
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is ChangeDueCalculatorViewModel.UiState.Loading -> {
                        Text(text = stringResource(R.string.loading), style = MaterialTheme.typography.h6)
                    }
                    is ChangeDueCalculatorViewModel.UiState.Success -> {
                        Text("TODO...", style = MaterialTheme.typography.body1)
                    }
                    is ChangeDueCalculatorViewModel.UiState.Error -> {
                        Text(text = stringResource(R.string.error_generic), style = MaterialTheme.typography.h6)
                    }
                }
            }
        }
    }
}
