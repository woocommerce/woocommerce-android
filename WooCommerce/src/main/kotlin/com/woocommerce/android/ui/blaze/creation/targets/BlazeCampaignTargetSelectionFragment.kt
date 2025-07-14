package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignTargetSelectionFragment : BaseFragment() {
    companion object {
        const val BLAZE_TARGET_SELECTION_RESULT = "blaze_target_selection_result"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    val viewModel: BlazeCampaignTargetSelectionViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignTargetSelectionScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is ExitWithResult<*> -> navigateBackWithResult(BLAZE_TARGET_SELECTION_RESULT, event.data)
            }
        }
    }
}
