package com.woocommerce.android.ui.customfields.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class CustomFieldsEditorFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    private val viewModel: CustomFieldsEditorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            CustomFieldsEditorScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CustomFieldsEditorViewModel.CopyContentToClipboard -> copyToClipboard(event)
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithResult(event.key!!, event.data)
                MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun copyToClipboard(event: CustomFieldsEditorViewModel.CopyContentToClipboard) {
        requireContext().copyToClipboard(getString(event.labelResource), event.content)
        ToastUtils.showToast(requireContext(), R.string.copied_to_clipboard)
    }
}
