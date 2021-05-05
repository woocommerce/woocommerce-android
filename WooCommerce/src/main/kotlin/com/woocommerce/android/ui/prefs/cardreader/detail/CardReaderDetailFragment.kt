package com.woocommerce.android.ui.prefs.cardreader.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderDetailBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderScanScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderDetailFragment : BaseFragment(R.layout.card_reader_detail) {
    val viewModel: CardReaderDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = CardReaderDetailBinding.bind(view)

        initViews(binding)
        initObservers()
    }

    private fun initViews(binding: CardReaderDetailBinding) {
        binding.testingText.setText("Hardcoded: Card Reader Detail Fragment")
        binding.initiateScanBtn.setOnClickListener {
            viewModel.onInitiateScanBtnClicked()
        }
    }

    private fun initObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer {
            when (it) {
                is CardReaderScanScreen ->
                    findNavController().navigateSafely(R.id.action_cardReaderDetailFragment_to_cardReaderScanFragment)
                else ->
                    it.isHandled = false
            }
        })
    }
}
