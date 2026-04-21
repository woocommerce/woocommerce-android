package com.woocommerce.android.ui.prefs.developer

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateFrequencyUiModel
import com.woocommerce.android.util.WooLog
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        logAllScheduledWork(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvents()
    }

    @Suppress("MagicNumber")
    private fun observeEvents() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowToastString -> {
                    ToastUtils.showToast(context, event.message)
                }

                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowToastText -> {
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

                is DeveloperOptionsViewModel.DeveloperOptionsEvents.OpenFeatureFlags -> {
                    findNavController().navigate(
                        DeveloperOptionsFragmentDirections.actionDeveloperOptionsFragmentToDevFeatureFlagsFragment()
                    )
                }

                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowFeatureAnnouncement -> {
                    findNavController().navigate(
                        DeveloperOptionsFragmentDirections
                            .actionDeveloperOptionsFragmentToFeatureAnnouncementDialogFragment(event.announcement)
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

    private fun logAllScheduledWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val workQuery = WorkQuery.Builder
            .fromStates(
                listOf(
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.CANCELLED,
                    WorkInfo.State.FAILED,
                    WorkInfo.State.SUCCEEDED
                )
            )
            .build()
        val workInfos = workManager.getWorkInfos(workQuery).get()

        if (workInfos.isEmpty()) {
            WooLog.d(WooLog.T.UTILS, "No work scheduled")
            return
        }

        WooLog.d(WooLog.T.UTILS, "=== Scheduled Work ===")
        workInfos.forEach { workInfo ->
            WooLog.d(WooLog.T.UTILS, "ID: ${workInfo.id}")
            WooLog.d(WooLog.T.UTILS, "  State: ${workInfo.state}")
            WooLog.d(WooLog.T.UTILS, "  Tags: ${workInfo.tags.joinToString()}")
            WooLog.d(WooLog.T.UTILS, "  Progress: ${workInfo.progress}")
            WooLog.d(WooLog.T.UTILS, "  RunAttemptCount: ${workInfo.runAttemptCount}")
            WooLog.d(WooLog.T.UTILS, "-----------------------")
        }
    }
}
