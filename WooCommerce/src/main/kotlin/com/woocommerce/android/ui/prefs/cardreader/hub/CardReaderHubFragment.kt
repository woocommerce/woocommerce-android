package com.woocommerce.android.ui.prefs.cardreader.hub

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment

class CardReaderHubFragment : BaseFragment(R.layout.fragment_card_reader_hub) {
    val viewModel: CardReaderHubViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvents()
        observeViewState()
    }

    private fun observeEvents() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                else -> event.isHandled = false
            }
        }
    }

    private fun observeViewState() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { state ->
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
