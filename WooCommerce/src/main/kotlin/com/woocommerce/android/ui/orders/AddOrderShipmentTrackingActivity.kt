package com.woocommerce.android.ui.orders

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_add_shipment_tracking.*
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.Calendar
import javax.inject.Inject

class AddOrderShipmentTrackingActivity : AppCompatActivity(), AddOrderShipmentTrackingContract.View,
        AddOrderTrackingProviderActionListener, HasSupportFragmentInjector {
    companion object {
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        const val FIELD_ORDER_TRACKING_NUMBER = "order-tracking-number"
        const val FIELD_ORDER_TRACKING_DATE_SHIPPED = "order-tracking-date-shipped"
        const val FIELD_ORDER_TRACKING_PROVIDER = "order-tracking-provider"
        const val FIELD_IS_CONFIRMING_DISCARD = "is_confirming_discard"
    }

    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var presenter: AddOrderShipmentTrackingContract.Presenter
    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    private var isConfirmingDiscard = false
    private lateinit var orderId: OrderIdentifier
    private var dateShippedPickerDialog: DatePickerDialog? = null
    private var providerListPickerDialog: AddOrderTrackingProviderListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shipment_tracking)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        title = getString(R.string.order_shipment_tracking_toolbar_title)

        if (savedInstanceState != null) {
            orderId = savedInstanceState.getString(FIELD_ORDER_IDENTIFIER) ?: ""
            addTracking_number.setText(savedInstanceState.getString(FIELD_ORDER_TRACKING_NUMBER, ""))
            addTracking_editCarrier.text = savedInstanceState.getString(FIELD_ORDER_TRACKING_PROVIDER, "")
            addTracking_date.text = savedInstanceState.getString(FIELD_ORDER_TRACKING_DATE_SHIPPED)
            if (savedInstanceState.getBoolean(FIELD_IS_CONFIRMING_DISCARD)) {
                confirmDiscard()
            }
        } else {
            orderId = intent.getStringExtra(FIELD_ORDER_IDENTIFIER) ?: ""
            intent.getStringExtra(FIELD_ORDER_TRACKING_PROVIDER)?.let { addTracking_editCarrier.text = it }
            val dateShipped = intent.getStringExtra(FIELD_ORDER_TRACKING_DATE_SHIPPED)?.let { it }
                    ?: DateUtils.getCurrentDateString()
            displayFormatDateShippedText(dateShipped)
        }

        presenter.takeView(this)

        /**
         * When date field is clicked, open calendar dialog with default date set to
         * current date if no date was previously selected
         */
        addTracking_date.setOnClickListener {
            val calendar = DateUtils.getCalendarInstance(getDateShippedText())
            dateShippedPickerDialog = DatePickerDialog(this,
                    DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                        val actualMonth = month + 1
                        displayFormatDateShippedText(String.format("$year-$actualMonth-$dayOfMonth"))
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            )
            dateShippedPickerDialog?.show()
        }

        /**
         * When carrier field is clicked, open dialog fragment to display list of providers
         */
        addTracking_editCarrier.setOnClickListener {
            providerListPickerDialog = AddOrderTrackingProviderListFragment
                    .newInstance(
                            selectedProviderText = getProviderText(),
                            orderIdentifier = orderId)
                    .also { it.show(supportFragmentManager, AddOrderTrackingProviderListFragment.TAG) }
        }
    }

    override fun onPause() {
        super.onPause()
        /**
         * If calendar dialog is displaying when activity is in a paused state,
         * then dismiss the dialog
         */
        dateShippedPickerDialog?.dismiss()
        dateShippedPickerDialog = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    /**
     * Reusing the same menu used for adding order notes
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_add -> {
                if (getProviderText().isEmpty()) {
                    addTracking_editCarrier.isFocusableInTouchMode = true
                    addTracking_editCarrier.requestFocus()
                    addTracking_editCarrier.error = getString(R.string.order_shipment_tracking_empty_provider)
                    addTracking_number.error = null
                    return true
                }

                if (addTracking_number.text.isNullOrEmpty()) {
                    addTracking_editCarrier.error = null
                    addTracking_number.error = getString(R.string.order_shipment_tracking_empty_tracking_num)
                    return true
                }

                if (!networkStatus.isConnected()) {
                    uiMessageResolver.showOfflineSnack()
                } else {
                    AnalyticsTracker.track(ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED)
                    val providerText = getProviderText()
                    val trackingNumText = addTracking_number.text.toString()
                    if (!providerText.isEmpty() && !trackingNumText.isEmpty()) {
                        val data = Intent()
                        data.putExtra(FIELD_ORDER_TRACKING_NUMBER, trackingNumText)
                        data.putExtra(FIELD_ORDER_TRACKING_DATE_SHIPPED, getDateShippedText())
                        data.putExtra(FIELD_ORDER_TRACKING_PROVIDER, providerText)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        if (getProviderText().isNotEmpty() || addTracking_number.text.toString().isNotEmpty()) {
            confirmDiscard()
        } else {
            onActivityFinish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(FIELD_ORDER_IDENTIFIER, orderId)
        outState?.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        outState?.putString(FIELD_ORDER_TRACKING_NUMBER, addTracking_number.text.toString())
        outState?.putString(FIELD_ORDER_TRACKING_DATE_SHIPPED, addTracking_date.text.toString())
        outState?.putString(FIELD_ORDER_TRACKING_PROVIDER, addTracking_editCarrier.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun confirmDiscard() {
        isConfirmingDiscard = true
        AlertDialog.Builder(this)
                .setMessage(R.string.order_shipment_tracking_confirm_discard)
                .setCancelable(true)
                .setPositiveButton(R.string.discard) { _, _ ->
                    onActivityFinish()
                }
                .setNegativeButton(R.string.keep_editing) { _, _ ->
                    isConfirmingDiscard = false
                }
                .show()
    }

    override fun onActivityFinish() {
        // set result here to pass the selected provider text and the flag for fetching provider list
        val intent = intent
        intent.putExtra(FIELD_ORDER_TRACKING_PROVIDER, getProviderText())
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    override fun getProviderText(): String {
        return addTracking_editCarrier.text.toString().trim()
    }

    override fun getDateShippedText(): String {
        // format displayed date to YYY-MM-dd i.e. from May 9, 2019 -> 2019-05-09
        return com.woocommerce.android.util.DateUtils.getDateString(addTracking_date.text.toString())
    }

    override fun onTrackingProviderSelected(selectedCarrierName: String) {
        addTracking_editCarrier.text = selectedCarrierName
        addTracking_editCarrier.error = null
        addTracking_editCarrier.isFocusableInTouchMode = false
        addTracking_editCarrier.isFocusable = false
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    private fun displayFormatDateShippedText(dateString: String) {
        addTracking_date.text = com.woocommerce.android.util.DateUtils.getLocalizedLongDateString(
                applicationContext,
                dateString
        )
    }
}
