package com.woocommerce.android.ui.prefs.developer

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateFrequencyUiModel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class DeveloperOptionsFragment : BaseFragment() {
    val viewModel: DeveloperOptionsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            DeveloperOptionsScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowToastString -> {
                    ToastUtils.showToast(context, event.message)
                }

                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowUpdateOptionsDialog -> {
                    showUpdateOptionsDialog(
                        values = event.options,
                        mapper = { requireContext().getString(it.title) },
                        selectedValue = event.selectedValue
                    )
                }

                is DeveloperOptionsViewModel.DeveloperOptionsEvents.OpenApiFaker -> {
                    findNavController().navigate(
                        DeveloperOptionsFragmentDirections.actionDeveloperOptionsFragmentToApiFaker()
                    )
                }
            }
        }
    }

    private fun showUpdateOptionsDialog(
        values: List<UpdateFrequencyUiModel>,
        mapper: (UpdateFrequencyUiModel) -> String,
        selectedValue: UpdateFrequencyUiModel
    ) {
        var currentlySelectedValue = selectedValue
        val textValues = values.map(mapper).toTypedArray()
        MaterialAlertDialogBuilder(
            ContextThemeWrapper(
                context,
                R.style.Theme_Woo_DayNight
            )
        )
            .setOnDismissListener {
                viewModel.onUpdateReaderOptionChanged(currentlySelectedValue)
            }
            .setSingleChoiceItems(textValues, selectedValue.ordinal) { _, which ->
                currentlySelectedValue = values[which]
            }.show()
    }

    override fun getFragmentTitle() = resources.getString(R.string.dev_options)
}
