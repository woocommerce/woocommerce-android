package com.woocommerce.android.ui.customfields.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.customfields.CustomFieldContentType
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.editor.CustomFieldsEditorFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CustomFieldsFragment : BaseFragment() {
    private val viewModel: CustomFieldsViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            CustomFieldsScreen(
                viewModel = viewModel
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
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun handleResults() {
        handleResult<CustomFieldUiModel>(CustomFieldsEditorFragment.RESULT_KEY) { result ->
            viewModel.onCustomFieldUpdated(result)
        }
    }

    private fun openEditor(field: CustomFieldUiModel) {
        findNavController().navigate(
            CustomFieldsFragmentDirections.actionCustomFieldsFragmentToCustomFieldsEditorFragment(
                customFieldId = field.id ?: -1,
                parentItemId = viewModel.parentItemId,
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
}
