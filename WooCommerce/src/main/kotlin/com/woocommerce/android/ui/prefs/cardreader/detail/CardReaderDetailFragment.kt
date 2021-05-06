package com.woocommerce.android.ui.prefs.cardreader.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderDetailBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderDetailFragment : BaseFragment(R.layout.fragment_card_reader_detail) {
    val viewModel: CardReaderDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderDetailBinding.bind(view)

        initViews(binding)
        initObservers()
    }

    private fun initViews(binding: FragmentCardReaderDetailBinding) {
        binding.testingText.setText("Hardcoded: Card Reader Detail Fragment")
        binding.connectBtn.setOnClickListener {
            viewModel.onConnectBtnClicked()
        }
    }

    private fun initObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer {
            when (it) {
                is CardReaderConnectScreen ->
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderDetailFragment_to_cardReaderConnectFragment)
                else ->
                    it.isHandled = false
            }
        })
    }
}
