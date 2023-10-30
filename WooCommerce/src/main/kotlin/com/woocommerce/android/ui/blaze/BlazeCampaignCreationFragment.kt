package com.woocommerce.android.ui.blaze

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: BlazeCampaignCreationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    BlazeCampaignCreationScreen(
                        viewModel = viewModel,
                        userAgent = viewModel.userAgent,
                        wpcomWebViewAuthenticator = viewModel.wpComWebViewAuthenticator,
                        activityRegistry = requireActivity().activityResultRegistry,
                        onClose = viewModel::onClose
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is BlazeCampaignCreationViewModel.CampaignCreated -> openBlazeCampaignList()
            }
        }
    }

    private fun openBlazeCampaignList() {
        findNavController().navigateSafely(
            BlazeCampaignCreationFragmentDirections.actionBlazeCampaignCreationFragmentToBlazeCampaignListFragment(
                isPostCampaignCreation = true
            )
        )
    }
}
