package com.woocommerce.android.ui.appwidgets.stats.today

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentTodayWidgetConfigureBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel.TodayWidgetNavigationTarget.ViewWidgetColorSelectionList
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel.TodayWidgetNavigationTarget.ViewWidgetSiteSelectionList
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel.WidgetAdded
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TodayWidgetConfigureFragment : BaseFragment() {
    private val uiMessageResolver by lazy {
        TodayWidgetUIMessageResolver(requireActivity() as TodayWidgetConfigurationActivity)
    }
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater

    private var _binding: FragmentTodayWidgetConfigureBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodayWidgetConfigureViewModel by navGraphViewModels(R.id.nav_graph_today_widget)

    override fun getFragmentTitle() = getString(R.string.stats_today_widget_configure_title)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTodayWidgetConfigureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()
        activity.setResult(AppCompatActivity.RESULT_CANCELED)
        val appWidgetId = activity.intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finishActivityWithResult(Activity.RESULT_CANCELED, appWidgetId)
            return
        }

        viewModel.start(appWidgetId)

        binding.todayWidgetSitePicker.setClickListener { viewModel.onSiteSpinnerSelected() }
        binding.todayWidgetColorPicker.setClickListener { viewModel.onColorSpinnerSelected() }
        binding.btnAddWidget.setOnClickListener { viewModel.addWidget() }
    }

    private fun setupObservers(viewModel: TodayWidgetConfigureViewModel) {
        viewModel.todayWidgetConfigureViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.selectedSiteUiModel.takeIfNotEqualTo(old?.selectedSiteUiModel) {
                it?.title?.let { title -> binding.todayWidgetSitePicker.setText(title) }
            }
            new.selectedWidgetColorCode.takeIfNotEqualTo(old?.selectedWidgetColorCode) {
                binding.todayWidgetColorPicker.setText(getString(it.label))
            }
            new.buttonEnabled.takeIfNotEqualTo(old?.buttonEnabled) {
                binding.btnAddWidget.isEnabled = it
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ViewWidgetSiteSelectionList -> {
                    findNavController().navigateSafely(
                        R.id.action_todayWidgetConfigureFragment_to_widgetSiteSelectionDialogFragment
                    )
                }
                is ViewWidgetColorSelectionList -> {
                    findNavController().navigateSafely(
                        R.id.action_todayWidgetConfigureFragment_to_widgetColorSelectionFragment
                    )
                }
                is WidgetAdded -> {
                    todayWidgetUpdater.updateAppWidget(requireContext(), event.appWidgetId)
                    finishActivityWithResult(Activity.RESULT_OK, event.appWidgetId)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun finishActivityWithResult(resultCode: Int, appWidgetId: Int) {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        activity?.setResult(resultCode, resultValue)
        activity?.finish()
    }
}
