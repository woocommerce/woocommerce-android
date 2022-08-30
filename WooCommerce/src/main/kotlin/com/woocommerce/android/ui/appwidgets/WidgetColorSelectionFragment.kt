package com.woocommerce.android.ui.appwidgets

import android.app.Dialog
import android.os.Bundle
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetColorSelectionFragment : DialogFragment() {
    private val viewModel: TodayWidgetConfigureViewModel by hiltNavGraphViewModels(R.id.nav_graph_today_widget)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_widget_color_selector, null) as RadioGroup
        view.check(viewModel.getSelectedColor().toViewId())
        view.setOnCheckedChangeListener { _, checkedId ->
            checkedId.toColor()?.let {
                viewModel.selectColor(it)
                dismiss()
            }
        }
        alertDialogBuilder.setView(view)
        alertDialogBuilder.setTitle(R.string.stats_today_widget_configure_color_hint)
        return alertDialogBuilder.create()
    }

    private fun Int.toColor(): WidgetColorMode? {
        return when (this) {
            R.id.stats_widget_light_color -> WidgetColorMode.LIGHT
            R.id.stats_widget_dark_color -> WidgetColorMode.DARK
            else -> null
        }
    }

    private fun WidgetColorMode.toViewId(): Int {
        return when (this) {
            WidgetColorMode.LIGHT -> R.id.stats_widget_light_color
            WidgetColorMode.DARK -> R.id.stats_widget_dark_color
        }
    }
}
