package com.woocommerce.android.ui.login.storecreation.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ThemeActivationFragmentDialog : DialogFragment() {
    private val viewModel: ThemeActivationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return composeView {
            ThemeActivationScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun ThemeActivationScreen(viewModel: ThemeActivationViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        TODO()
    }
}
