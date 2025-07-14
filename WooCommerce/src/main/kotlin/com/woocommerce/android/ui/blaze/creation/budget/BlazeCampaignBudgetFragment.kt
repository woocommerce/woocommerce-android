package com.woocommerce.android.ui.blaze.creation.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignBudgetFragment : BaseFragment() {
    companion object {
        const val EDIT_BUDGET_AND_DURATION_RESULT = "edit_budget_and_duration_result"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    val viewModel: BlazeCampaignBudgetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            CampaignBudgetScreen(viewModel)
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
                is ExitWithResult<*> -> {
                    navigateBackWithResult(
                        EDIT_BUDGET_AND_DURATION_RESULT,
                        event.data as Budget
                    )
                }
            }
        }
    }
}
