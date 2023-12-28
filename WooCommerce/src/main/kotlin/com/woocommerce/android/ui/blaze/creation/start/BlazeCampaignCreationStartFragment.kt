package com.woocommerce.android.ui.blaze.creation.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationStartFragment : BaseFragment() {
    private val viewModel: BlazeCampaignCreationStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleEvents()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        TODO("Implement loading view for making the AI prompt before showing the AD preview screen")
    }

    private fun handleEvents() {
        // Use the fragment as the lifecycle owner since navigation might happen before the view is created
        viewModel.event.observe(this) { event ->
            when (event) {
                is BlazeCampaignCreationStartViewModel.ShowBlazeCampaignCreationIntro ->
                    navigateToBlazeCampaignCreationIntro()
            }
        }
    }

    private fun navigateToBlazeCampaignCreationIntro() {
        TODO()
    }
}
