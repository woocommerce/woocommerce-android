package com.woocommerce.android.ui.payments.changeduecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeDueCalculatorFragment : BaseFragment() {

    private val viewModel: ChangeDueCalculatorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                ChangeDueCalculatorScreen(uiState = uiState, onNavigateUp = {
                    findNavController().navigateUp()
                }, onCompleteOrderClick = {})
            }
        }
    }
}
