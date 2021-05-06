package com.woocommerce.android.ui.prefs.cardreader.connect

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderConnectBinding
import com.woocommerce.android.ui.base.BaseFragment

class CardReaderConnectFragment : BaseFragment(R.layout.fragment_card_reader_connect) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderConnectBinding.bind(view)
        binding.testingText.setText("Hardcoded: Card Reader Connect Fragment")
    }
}
