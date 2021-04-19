package com.woocommerce.android.ui.prefs.cardreader

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSettingsCardReaderConnectBinding

class CardReaderConnectFragment : Fragment(R.layout.fragment_settings_card_reader_connect) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsCardReaderConnectBinding.bind(view)
        binding.testingText.setText("Hardcoded: Card Reader Connect Fragment")
    }
}
