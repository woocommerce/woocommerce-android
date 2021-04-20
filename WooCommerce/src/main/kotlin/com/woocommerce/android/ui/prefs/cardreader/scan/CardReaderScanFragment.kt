package com.woocommerce.android.ui.prefs.cardreader.scan

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSettingsCardReaderScanBinding
import com.woocommerce.android.ui.base.BaseFragment

class CardReaderScanFragment : BaseFragment(R.layout.fragment_settings_card_reader_scan) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsCardReaderScanBinding.bind(view)
        binding.testingTextScan.setText("Hardcoded: Card Reader Scan Fragment")
    }
}
