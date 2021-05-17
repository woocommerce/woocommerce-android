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
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderDetailFragment : BaseFragment(R.layout.card_reader_detail) {
    val viewModel: CardReaderDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = CardReaderDetailBinding.bind(view)

        initObservers(binding)
    }

    private fun initObservers(binding: CardReaderDetailBinding) {
        viewModel.event.observe(viewLifecycleOwner, Observer {
            when (it) {
                is CardReaderConnectScreen ->
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderDetailFragment_to_cardReaderConnectFragment)
                else ->
                    it.isHandled = false
            }
        })

        viewModel.viewStateData.observe(viewLifecycleOwner, Observer {
            UiHelpers.setTextOrHide(binding.headerLabel, it.headerLabel)
            UiHelpers.setImageOrHide(binding.illustration, it.illustration)
            UiHelpers.setTextOrHide(binding.firstHintLabel, it.firstHintLabel)
            UiHelpers.setTextOrHide(binding.secondHintLabel, it.secondHintLabel)
            UiHelpers.setTextOrHide(binding.connectBtn, it.connectBtnLabel)
            binding.connectBtn.setOnClickListener { _ -> it.onPrimaryActionClicked?.invoke() }
        })
    }
}
