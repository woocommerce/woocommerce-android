package com.woocommerce.android.ui.login.storecreation.countrypicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CountryListPickerFragment: BaseFragment() {

    private val viewModel: CountryListPickerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    CountryPickerSelectorBottomSheet(viewModel = viewModel)
                }
            }
        }
    }
}
