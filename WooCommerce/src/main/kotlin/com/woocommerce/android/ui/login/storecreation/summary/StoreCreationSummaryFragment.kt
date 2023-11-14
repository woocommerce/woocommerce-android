package com.woocommerce.android.ui.login.storecreation.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnCancelPressed
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnStoreCreationFailure
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnStoreCreationSuccess
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoreCreationSummaryFragment : BaseFragment() {
    private val viewModel: StoreCreationSummaryViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                StoreCreationSummaryScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventObservers()
    }

    private fun setupEventObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnCancelPressed -> findNavController().popBackStack()
                is OnStoreCreationSuccess -> findNavController().navigateSafely(
                    StoreCreationSummaryFragmentDirections.actionSummaryFragmentToStoreProfilerIndustriesFragment()
                )
                is OnStoreCreationFailure -> displayStoreCreationErrorDialog()
            }
        }
    }

    private fun displayStoreCreationErrorDialog() {
        WooDialog.showDialog(
            activity = requireActivity(),
            titleId = R.string.free_trial_summary_store_creation_error_title,
            messageId = R.string.free_trial_summary_store_creation_error_message,
            positiveButtonId = R.string.free_trial_summary_store_creation_error_dialog_action
        )
    }
}
