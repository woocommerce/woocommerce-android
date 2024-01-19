package com.woocommerce.android.ui.blaze.creation.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdFragment
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdViewModel.EditAdResult
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToEditAdScreen
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationPreviewFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    val viewModel: BlazeCampaignCreationPreviewViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignCreationPreviewScreen(viewModel = viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
        handleResults()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is NavigateToEditAdScreen -> findNavController().navigate(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignCreationEditAdFragment(
                            event.tagline,
                            event.description,
                            event.campaignImageUrl
                        )
                )
            }
        }
    }

    private fun handleResults() {
        handleResult<EditAdResult>(BlazeCampaignCreationEditAdFragment.EDIT_AD_RESULT) {
            viewModel.onAdUpdated(it.tagline, it.description, it.campaignImageUrl)
        }
    }
}
