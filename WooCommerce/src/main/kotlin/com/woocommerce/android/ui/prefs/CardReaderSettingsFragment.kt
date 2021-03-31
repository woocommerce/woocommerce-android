package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsCardReaderBinding

class CardReaderSettingsFragment : Fragment(R.layout.fragment_settings_card_reader) {
    companion object {
        const val TAG = "card-reader-settings"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsCardReaderBinding.bind(view)
        binding.connectReaderButton.setOnClickListener {
            // TODO card reader implement connect reader button
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings_card_reader)
    }
}
