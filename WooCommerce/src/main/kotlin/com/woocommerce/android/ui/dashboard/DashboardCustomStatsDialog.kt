package com.woocommerce.android.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RadioButton
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.getCheckValue
import com.woocommerce.android.ui.dashboard.DashboardCustomStatsDialog.CustomStatsFieldListener
import kotlinx.android.synthetic.main.dashboard_custom_stats_dialog.*
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import java.util.Date

/**
 * Dialog displays a date range picker and list of [StatsGranularity]
 * and allows for selecting start date, end date and granularity for order stats.
 *
 * This fragment should be instantiated using the [DashboardCustomStatsDialog.newInstance] method.
 * Calling classes can obtain the results of selection through the [CustomStatsFieldListener].
 */
class DashboardCustomStatsDialog : DialogFragment() {
    companion object {
        const val TAG: String = "DashboardCustomStatsDialog"

        fun newInstance(
            wcOrderStatsModel: WCOrderStatsModel?,
            listener: CustomStatsFieldListener
        ): DashboardCustomStatsDialog {
            val fragment = DashboardCustomStatsDialog()
            fragment.wcOrderStatsModel = wcOrderStatsModel
            fragment.listener = listener
            return fragment
        }
    }

    interface CustomStatsFieldListener {
        fun onFieldSelected(startDate: String, endDate: String, granularity: StatsGranularity)
    }

    private var listener: CustomStatsFieldListener? = null
    private var wcOrderStatsModel: WCOrderStatsModel? = null
    private var startDatePickerDialog: DatePickerDialog? = null
    private var endDatePickerDialog: DatePickerDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dashboard_custom_stats_dialog, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        for (i in 0 until radio_group.childCount) {
            (radio_group.getChildAt(i) as? RadioButton)?.text = StatsGranularity.values()[i].name
        }

        val startDate = wcOrderStatsModel?.startDate ?: DateUtils.getCurrentDateString()
        stats_from_date.text = startDate

        val endDate = wcOrderStatsModel?.endDate ?: DateUtils.getCurrentDateString()
        stats_to_date.text = endDate

        val granularity = wcOrderStatsModel?.unit?.let {
            StatsGranularity.fromString(it).ordinal
        } ?: 0
        (radio_group.getChildAt(granularity) as? RadioButton)?.isChecked = true

        stats_from_date.setOnClickListener {
            startDatePickerDialog = displayDialog(
                    DateUtils.getCalendarInstance(stats_from_date.text.toString()), stats_from_date)
        }

        stats_to_date.setOnClickListener {
            endDatePickerDialog = displayDialog(
                    DateUtils.getCalendarInstance(stats_to_date.text.toString()), stats_to_date)
        }

        stats_dialog_ok.setOnClickListener {
            listener?.onFieldSelected(
                    stats_from_date.text.toString(),
                    stats_to_date.text.toString(),
                    StatsGranularity.valueOf(radio_group.getCheckValue().toUpperCase())
            )
            dismiss()
        }

        stats_dialog_cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun displayDialog(
        calendar: Calendar,
        button: AppCompatButton
    ): DatePickerDialog {
        val datePicker = DatePickerDialog(requireActivity(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    button.text = DateUtils.getFormattedDateString(year, month, dayOfMonth)
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.maxDate = Date().time
        datePicker.show()
        return datePicker
    }

    override fun onPause() {
        super.onPause()
        startDatePickerDialog?.dismiss()
        startDatePickerDialog = null

        endDatePickerDialog?.dismiss()
        endDatePickerDialog = null
    }

    override fun onResume() {
        super.onResume()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        AnalyticsTracker.trackViewShown(this)
    }
}
