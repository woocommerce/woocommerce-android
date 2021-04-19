package com.woocommerce.android.ui.prefs.cardreader

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSettingsCardReaderConnectBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class CardReaderConnectFragment : BaseFragment(R.layout.fragment_settings_card_reader_connect) {

    @Inject lateinit var viewModelFactory: ViewModelFactory

    val viewModel: CardReaderConnectViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsCardReaderConnectBinding.bind(view)
        binding.testingText.setText("Hardcoded: Card Reader Connect Fragment")
        viewModel.foo()
    }
}
