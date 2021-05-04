package com.woocommerce.android.ui.prefs.cardreader.connect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSettingsCardReaderConnectBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.NavigationTarget.CardReaderScanScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderConnectFragment : BaseFragment(R.layout.fragment_settings_card_reader_connect) {
    val viewModel: CardReaderConnectViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsCardReaderConnectBinding.bind(view)

        initViews(binding)
        initObservers()
    }

    private fun initViews(binding: FragmentSettingsCardReaderConnectBinding) {
        binding.testingText.setText("Hardcoded: Card Reader Connect Fragment")
        binding.initiateScanBtn.setOnClickListener {
            viewModel.onInitiateScanBtnClicked()
        }
    }

    private fun initObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer {
            when (it) {
                is CardReaderScanScreen ->
                    findNavController().navigateSafely(R.id.action_cardReaderConnectFragment_to_cardReaderScanFragment)
                else ->
                    it.isHandled = false
            }
        })
    }
}
