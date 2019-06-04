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
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.AppRatingDialog
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
        const val FIELD_IS_CONFIRMING_DISCARD = "is-confirming-discard"
        const val FIELD_IS_CUSTOM_PROVIDER = "is-custom-provider"
        const val FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_NAME = "order-tracking-custom-provider-name"
        const val FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL = "order-tracking-custom-provider-url"
    }

    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var presenter: AddOrderShipmentTrackingContract.Presenter
    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    private var isConfirmingDiscard = false
    private var isSelectedProviderCustom = false
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
            isSelectedProviderCustom = savedInstanceState.getBoolean(FIELD_IS_CUSTOM_PROVIDER, false)
            addTracking_number.setText(savedInstanceState.getString(FIELD_ORDER_TRACKING_NUMBER, ""))
            addTracking_date.text = savedInstanceState.getString(FIELD_ORDER_TRACKING_DATE_SHIPPED)
            addTracking_custom_provider_name.setText(
                    savedInstanceState.getString(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_NAME, "")
            )
            addTracking_custom_provider_url.setText(
                    savedInstanceState.getString(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL, "")
            )
            if (savedInstanceState.getBoolean(FIELD_IS_CONFIRMING_DISCARD)) {
                confirmDiscard()
            }
        } else {
            orderId = intent.getStringExtra(FIELD_ORDER_IDENTIFIER) ?: ""
            isSelectedProviderCustom = intent.getBooleanExtra(FIELD_IS_CUSTOM_PROVIDER, false)
            val dateShipped = intent.getStringExtra(FIELD_ORDER_TRACKING_DATE_SHIPPED)?.let { it }
                    ?: DateUtils.getCurrentDateString()
            displayFormatDateShippedText(dateShipped)
        }

        val selectedCarrierName = savedInstanceState?.let {
            savedInstanceState.getString(FIELD_ORDER_TRACKING_PROVIDER)
        } ?: intent.getStringExtra(FIELD_ORDER_TRACKING_PROVIDER)?.let { it } ?: ""
        if (isCustomProvider()) {
            addTracking_custom_provider_name.setText(selectedCarrierName)
            addTracking_editCarrier.text = getString(R.string.order_shipment_tracking_custom_provider_section_name)
            showCustomProviderFields()
        } else {
            addTracking_editCarrier.text = selectedCarrierName
            hideCustomProviderFields()
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
                            selectedProviderText = addTracking_editCarrier.text.toString(),
                            orderIdentifier = orderId)
                    .also { it.show(supportFragmentManager, AddOrderTrackingProviderListFragment.TAG) }
        }
    }

    override fun onPause() {
        super.onPause()
        /**
         * If calendar dialog or provider list dialog is displaying when activity is in a paused state,
         * then dismiss the dialog
         */
        dateShippedPickerDialog?.dismiss()
        dateShippedPickerDialog = null

        providerListPickerDialog?.dismiss()
        providerListPickerDialog = null
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
                if (addTracking_editCarrier.text.toString().isEmpty()) {
                    addTracking_editCarrier.isFocusableInTouchMode = true
                    addTracking_editCarrier.requestFocus()
                    addTracking_editCarrier.error = getString(R.string.order_shipment_tracking_empty_provider)
                    addTracking_number.error = null
                    addTracking_custom_provider_name.error = null
                    return true
                }

                if (isCustomProvider() && addTracking_custom_provider_name.text.toString().isEmpty()) {
                    addTracking_number.error = null
                    addTracking_editCarrier.error = null
                    addTracking_custom_provider_name.requestFocus()
                    addTracking_custom_provider_name.error = getString(
                            R.string.order_shipment_tracking_empty_custom_provider_name
                    )
                    return true
                }

                if (addTracking_number.text.isNullOrEmpty()) {
                    addTracking_editCarrier.error = null
                    addTracking_custom_provider_name.error = null
                    addTracking_number.requestFocus()
                    addTracking_number.error = getString(R.string.order_shipment_tracking_empty_tracking_num)
                    return true
                }

                if (!networkStatus.isConnected()) {
                    uiMessageResolver.showOfflineSnack()
                } else {
                    AnalyticsTracker.track(ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED)
                    AppRatingDialog.incrementInteractions()

                    val providerText = getProviderText()
                    val trackingNumText = addTracking_number.text.toString()
                    val customProviderTrackingUrl = addTracking_custom_provider_url.text.toString()

                    val data = Intent()
                    data.putExtra(FIELD_ORDER_TRACKING_NUMBER, trackingNumText)
                    data.putExtra(FIELD_ORDER_TRACKING_DATE_SHIPPED, getDateShippedText())
                    data.putExtra(FIELD_ORDER_TRACKING_PROVIDER, providerText)
                    data.putExtra(FIELD_IS_CUSTOM_PROVIDER, isCustomProvider())
                    data.putExtra(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL, customProviderTrackingUrl)
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        if (addTracking_editCarrier.text.toString().isNotEmpty() || addTracking_number.text.toString().isNotEmpty() ||
                (isCustomProvider() && (
                        addTracking_custom_provider_name.text.toString().isNotEmpty() ||
                                addTracking_custom_provider_url.text.toString().isNotEmpty()
                        ))) {
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
        outState?.putBoolean(FIELD_IS_CUSTOM_PROVIDER, isCustomProvider())
        outState?.putString(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_NAME, addTracking_custom_provider_name.text.toString())
        outState?.putString(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL, addTracking_custom_provider_url.text.toString())
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
        return if (isCustomProvider()) {
            addTracking_custom_provider_name.text.toString()
        } else {
            addTracking_editCarrier.text.toString().trim()
        }
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
        isSelectedProviderCustom = addTracking_editCarrier.text ==
                getString(R.string.order_shipment_tracking_custom_provider_section_name)
        /**
         * Display custom provider fields only if
         * @param selectedCarrierName = custom provider
         */
        if (isCustomProvider()) {
            showCustomProviderFields()
        } else {
            hideCustomProviderFields()
        }
    }

    override fun isCustomProvider(): Boolean {
        return isSelectedProviderCustom
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    private fun displayFormatDateShippedText(dateString: String) {
        addTracking_date.text = com.woocommerce.android.util.DateUtils.getLocalizedLongDateString(
                applicationContext,
                dateString
        )
    }

    private fun showCustomProviderFields() {
        addTracking_custom_provider_name_view.visibility = View.VISIBLE
        addTracking_custom_provider_url_view.visibility = View.VISIBLE
    }

    private fun hideCustomProviderFields() {
        addTracking_custom_provider_name_view.visibility = View.GONE
        addTracking_custom_provider_url_view.visibility = View.GONE
    }
}
