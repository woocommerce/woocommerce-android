package com.woocommerce.android.ui.blaze.creation.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdFragment
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdViewModel.EditAdResult
import com.woocommerce.android.ui.blaze.creation.budget.BlazeCampaignBudgetFragment
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationFragment
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToAdDestinationScreen
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToBudgetScreen
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToEditAdScreen
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToPaymentSummary
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToTargetLocationSelectionScreen
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.NavigateToTargetSelectionScreen
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetLocationSelectionFragment
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetLocationSelectionViewModel.TargetLocationResult
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionFragment
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.TargetSelectionResult
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
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        handleResults()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is NavigateToBudgetScreen -> findNavController().navigateSafely(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignBudgetFragment(event.budget)
                )

                is NavigateToEditAdScreen -> findNavController().navigateSafely(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignCreationEditAdFragment(
                            event.productId,
                            event.tagLine,
                            event.description,
                            event.campaignImageUrl
                        )
                )

                is NavigateToTargetSelectionScreen -> findNavController().navigateSafely(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignTargetSelectionFragment(
                            event.targetType,
                            event.selectedIds.toTypedArray()
                        )
                )

                is NavigateToTargetLocationSelectionScreen -> findNavController().navigateSafely(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignTargetLocationSelectionFragment(
                            event.locations.toTypedArray()
                        )
                )

                is NavigateToAdDestinationScreen -> findNavController().navigateSafely(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignCreationAdDestinationFragment(
                            event.targetUrl,
                            event.productId
                        )
                )
                is NavigateToPaymentSummary -> findNavController().navigateSafely(
                    BlazeCampaignCreationPreviewFragmentDirections
                        .actionBlazeCampaignCreationPreviewFragmentToBlazeCampaignPaymentSummaryFragment(
                            event.budget
                        )
                )
            }
        }
    }

    private fun handleResults() {
        handleResult<EditAdResult>(BlazeCampaignCreationEditAdFragment.EDIT_AD_RESULT) {
            viewModel.onAdUpdated(it.tagline, it.description, it.campaignImageUrl)
        }
        handleResult<Budget>(BlazeCampaignBudgetFragment.EDIT_BUDGET_AND_DURATION_RESULT) {
            viewModel.onBudgetAndDurationUpdated(it)
        }
        handleResult<TargetSelectionResult>(BlazeCampaignTargetSelectionFragment.BLAZE_TARGET_SELECTION_RESULT) {
            viewModel.onTargetSelectionUpdated(it.targetType, it.selectedIds)
        }
        handleResult<TargetLocationResult>(BlazeCampaignTargetLocationSelectionFragment.BLAZE_TARGET_LOCATION_RESULT) {
            viewModel.onTargetLocationsUpdated(it.locations)
        }
        handleResult<String>(BlazeCampaignCreationAdDestinationFragment.BLAZE_DESTINATION_RESULT) {
            viewModel.onDestinationUrlUpdated(it)
        }
    }
}
