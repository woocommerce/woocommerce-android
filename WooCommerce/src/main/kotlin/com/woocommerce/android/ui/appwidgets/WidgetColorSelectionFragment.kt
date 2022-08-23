package com.woocommerce.android.ui.widgets

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.navGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class WidgetColorSelectionFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: TodayWidgetConfigureViewModel
        by navGraphViewModels(R.id.nav_graph_today_widget) { viewModelFactory }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = MaterialAlertDialogBuilder(activity)
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
