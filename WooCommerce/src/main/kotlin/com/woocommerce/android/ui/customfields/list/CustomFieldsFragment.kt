package com.woocommerce.android.ui.customfields.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.customfields.CustomFieldContentType
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.editor.CustomFieldsEditorViewModel
import com.woocommerce.android.ui.customfields.editor.CustomFieldsEditorViewModel.CustomFieldUpdateResult
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomFieldsFragment : BaseFragment() {
    private val viewModel: CustomFieldsViewModel by viewModels()

    private val snackbarHostState = SnackbarHostState()

    override val activityAppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            CustomFieldsScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
        handleResults()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CustomFieldsViewModel.OpenCustomFieldEditor -> openEditor(event.field)
                is CustomFieldsViewModel.CustomFieldValueClicked -> handleValueClick(event.field)
                is MultiLiveEvent.Event.ShowSnackbar -> showSnackbar(getString(event.message))
                is MultiLiveEvent.Event.ShowActionSnackbar -> showSnackbar(
                    message = event.message,
                    actionText = event.actionText,
                    action = { event.action.onClick(null) }
                )

                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun handleResults() {
        handleResult<CustomFieldUiModel>(CustomFieldsEditorViewModel.CUSTOM_FIELD_CREATED_RESULT_KEY) { result ->
            viewModel.onCustomFieldInserted(result)
        }
        handleResult<CustomFieldUpdateResult>(CustomFieldsEditorViewModel.CUSTOM_FIELD_UPDATED_RESULT_KEY) { result ->
            viewModel.onCustomFieldUpdated(result.oldKey, result.updatedField)
        }
        handleResult<CustomFieldUiModel>(CustomFieldsEditorViewModel.CUSTOM_FIELD_DELETED_RESULT_KEY) { result ->
            viewModel.onCustomFieldDeleted(result)
        }
    }

    private fun openEditor(field: CustomFieldUiModel?) {
        findNavController().navigate(
            CustomFieldsFragmentDirections.actionCustomFieldsFragmentToCustomFieldsEditorFragment(
                parentItemId = viewModel.parentItemId,
                customField = field
            )
        )
    }

    private fun handleValueClick(field: CustomFieldUiModel) {
        when (field.contentType) {
            CustomFieldContentType.URL -> ChromeCustomTabUtils.launchUrl(requireContext(), field.value)
            CustomFieldContentType.EMAIL -> ActivityUtils.sendEmail(requireContext(), field.value)
            CustomFieldContentType.PHONE -> ActivityUtils.dialPhoneNumber(requireContext(), field.value)
            CustomFieldContentType.TEXT -> error("Values of type TEXT should not be clickable")
        }
    }

    private fun showSnackbar(
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = snackbarHostState.showSnackbar(message = message, actionLabel = actionText)
            if (actionText != null && action != null && result == SnackbarResult.ActionPerformed) {
                action()
            }
        }
    }
}
