package com.woocommerce.android.ui.blaze.creation.destination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeRepository.DestinationParameters
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersFragment.Companion.BLAZE_DESTINATION_PARAMETERS_RESULT
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationViewModel.NavigateToParametersScreen
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationAdDestinationFragment : BaseFragment() {
    companion object {
        const val BLAZE_DESTINATION_RESULT = "blaze_destination_result"
    }
    private val viewModel: BlazeCampaignCreationAdDestinationViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignCreationAdDestinationScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
        handleResults()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> navigateBackWithResult(BLAZE_DESTINATION_RESULT, event.data)
                is NavigateToParametersScreen -> {
                    val action = BlazeCampaignCreationAdDestinationFragmentDirections
                        .actionAdDestinationFragmentToAdDestinationParametersFragment(event.destinationParameters)
                    findNavController().navigateSafely(action)
                }
            }
        }
    }

    private fun handleResults() {
        handleResult<DestinationParameters>(BLAZE_DESTINATION_PARAMETERS_RESULT) {
            viewModel.onDestinationParametersUpdated(it.targetUrl, it.parameters)
        }
    }
}
