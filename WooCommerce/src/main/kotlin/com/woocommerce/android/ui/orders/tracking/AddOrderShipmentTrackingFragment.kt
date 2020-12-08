package com.woocommerce.android.ui.orders.tracking

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED
import com.woocommerce.android.databinding.FragmentAddShipmentTrackingBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AppRatingDialog
import kotlinx.android.synthetic.main.fragment_add_shipment_tracking.*
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.util.ActivityUtils
import java.util.Calendar
import javax.inject.Inject
import org.wordpress.android.fluxc.utils.DateUtils as FluxCDateUtils

class AddOrderShipmentTrackingFragment : BaseFragment(R.layout.fragment_add_shipment_tracking),
    AddOrderShipmentTrackingContract.View,
    AddOrderTrackingProviderActionListener,
    BackPressListener {
    companion object {
        const val FIELD_ORDER_TRACKING_NUMBER = "order-tracking-number"
        const val FIELD_ORDER_TRACKING_DATE_SHIPPED = "order-tracking-date-shipped"
        const val FIELD_ORDER_TRACKING_PROVIDER = "order-tracking-provider"
        const val FIELD_IS_CONFIRMING_DISCARD = "is-confirming-discard"
        const val FIELD_IS_CUSTOM_PROVIDER = "is-custom-provider"
        const val FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_NAME = "order-tracking-custom-provider-name"
        const val FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL = "order-tracking-custom-provider-url"
        const val KEY_ADD_SHIPMENT_TRACKING_RESULT = "key_add_shipment_tracking_result"
    }

    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: AddOrderShipmentTrackingViewModel by viewModels { viewModelFactory }

    private var isConfirmingDiscard = false
    private var shouldShowDiscardDialog = true
    private var isSelectedProviderCustom = false
    private lateinit var orderId: OrderIdentifier
    private var dateShippedPickerDialog: DatePickerDialog? = null
    private var providerListPickerDialog: AddOrderTrackingProviderListFragment? = null

    private val navArgs: AddOrderShipmentTrackingFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_shipment_tracking, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.order_shipment_tracking_toolbar_title)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        orderId = navArgs.orderId

        if (savedInstanceState != null) {
            isSelectedProviderCustom = savedInstanceState.getBoolean(FIELD_IS_CUSTOM_PROVIDER, false)
            addTracking_number.setText(savedInstanceState.getString(FIELD_ORDER_TRACKING_NUMBER, ""))
            addTracking_date.setText(savedInstanceState.getString(FIELD_ORDER_TRACKING_DATE_SHIPPED, ""))
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
            isSelectedProviderCustom = navArgs.isCustomProvider
            val dateShipped = FluxCDateUtils.getCurrentDateString()
            displayFormatDateShippedText(dateShipped)
        }

        val selectedCarrierName = savedInstanceState?.let {
            savedInstanceState.getString(FIELD_ORDER_TRACKING_PROVIDER)
        } ?: navArgs.orderTrackingProvider
        if (isCustomProvider()) {
            addTracking_custom_provider_name.setText(selectedCarrierName)
            addTracking_editCarrier.setText(getString(R.string.order_shipment_tracking_custom_provider_section_name))
            showCustomProviderFields()
        } else {
            addTracking_editCarrier.setText(selectedCarrierName)
            hideCustomProviderFields()
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAddShipmentTrackingBinding.bind(view)
        initUi(binding)
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentAddShipmentTrackingBinding) {
        viewModel.addOrderShipmentTrackingViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.carrier.takeIfNotEqualTo(old?.carrier) {
                if (it.isCustom) {
                    showCustomProviderFields()
                    binding.addTrackingEditCarrier.setText(getString(R.string.order_shipment_tracking_custom_provider_section_name))
                    if (binding.addTrackingCustomProviderName.text.toString() != it.name) {
                        binding.addTrackingCustomProviderName.setText(it.name)
                    }
                } else {
                    hideCustomProviderFields()
                    binding.addTrackingEditCarrier.setText(it.name)
                }
            }
            new.trackingNumber.takeIfNotEqualTo(old?.trackingNumber) {
                if (binding.addTrackingNumber.text.toString() != it) {
                    binding.addTrackingNumber.setText(it)
                }
            }
            new.trackingLink.takeIfNotEqualTo(old?.trackingLink) {
                if (binding.addTrackingCustomProviderUrl.text.toString() != it) {
                    binding.addTrackingCustomProviderUrl.setText(it)
                }
            }
            new.date.takeIfNotEqualTo(old?.date) {
                binding.addTrackingDate.setText(DateUtils().getLocalizedLongDateString(requireActivity(), it))
            }
        }
    }

    private fun initUi(binding: FragmentAddShipmentTrackingBinding) {
        binding.addTrackingEditCarrier.setOnClickListener {
            providerListPickerDialog = AddOrderTrackingProviderListFragment
                .newInstance(
                    listener = this,
                    selectedProviderText = binding.addTrackingEditCarrier.text.toString(),
                    orderIdentifier = orderId
                )
                .also { it.show(parentFragmentManager, AddOrderTrackingProviderListFragment.TAG) }
        }

        binding.addTrackingDate.setOnClickListener {
            val calendar = FluxCDateUtils.getCalendarInstance(viewModel.currentSelectedDate)
            dateShippedPickerDialog = DatePickerDialog(
                requireActivity(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    viewModel.onDateChanged("$year-${month + 1}-$dayOfMonth")
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            )
            dateShippedPickerDialog?.show()
        }

        binding.addTrackingCustomProviderName.doOnTextChanged { text, _, _, _ ->
            viewModel.onCustomCarrierNameEntered(text.toString())
        }
        binding.addTrackingNumber.doOnTextChanged { text, _, _, _ ->
            viewModel.onTrackingNumberEntered(text.toString())
        }
        binding.addTrackingCustomProviderUrl.doOnTextChanged { text, _, _, _ ->
            viewModel.onTrackingLinkEntered(text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        dateShippedPickerDialog?.dismiss()
        dateShippedPickerDialog = null

        providerListPickerDialog?.dismiss()
        providerListPickerDialog = null
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    /**
     * Reusing the same menu used for adding order notes
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_add, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add -> {
                if (addTracking_editCarrier.text.toString().isEmpty()) {
                    addTracking_carrierLayout.error = getString(R.string.order_shipment_tracking_empty_provider)
                    addTracking_numberLayout.error = null
                    addTracking_customNameLayout.error = null
                    return true
                }

                if (isCustomProvider() && addTracking_custom_provider_name.text.toString().isEmpty()) {
                    addTracking_number.error = null
                    addTracking_carrierLayout.error = null
                    addTracking_custom_provider_name.requestFocus()
                    addTracking_customNameLayout.error = getString(
                        R.string.order_shipment_tracking_empty_custom_provider_name
                    )
                    return true
                }

                if (addTracking_number.text.isNullOrEmpty()) {
                    addTracking_carrierLayout.error = null
                    addTracking_customNameLayout.error = null
                    addTracking_number.requestFocus()
                    addTracking_numberLayout.error = getString(R.string.order_shipment_tracking_empty_tracking_num)
                    return true
                }

                AnalyticsTracker.track(ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED)
                AppRatingDialog.incrementInteractions()

                val providerText = getProviderText()
                val trackingNumText = addTracking_number.text.toString()
                val isCustomProvider = isCustomProvider()
                val customProviderTrackingUrl = addTracking_custom_provider_url.text.toString()

                AppPrefs.setSelectedShipmentTrackingProviderName(providerText)
                AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(isCustomProvider)

                val shipmentTracking = OrderShipmentTracking(
                    trackingNumber = trackingNumText,
                    dateShipped = getDateShippedText(),
                    trackingProvider = providerText,
                    isCustomProvider = isCustomProvider,
                    trackingLink = if (isCustomProvider) {
                        customProviderTrackingUrl
                    } else ""
                )

                shouldShowDiscardDialog = false
                navigateBackWithResult(KEY_ADD_SHIPMENT_TRACKING_RESULT, shipmentTracking)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Prevent back press in the main activity if the user entered a note so we can confirm the discard
     */
    override fun onRequestAllowBackPress(): Boolean {
        val providerText = addTracking_editCarrier.text.toString()
        val trackingNumText = addTracking_number.text.toString()
        val customProviderText = addTracking_custom_provider_name.text.toString()
        val customTrackingUrlText = addTracking_custom_provider_url.text.toString()
        val displayConfirmDialog = providerText.isNotEmpty() || trackingNumText.isNotEmpty() ||
            (isCustomProvider() && (customProviderText.isNotEmpty() || customTrackingUrlText.isNotEmpty()))

        return if (displayConfirmDialog && shouldShowDiscardDialog) {
            confirmDiscard()
            false
        } else {
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        outState.putBoolean(FIELD_IS_CUSTOM_PROVIDER, isCustomProvider())

        addTracking_number?.let {
            outState.putString(FIELD_ORDER_TRACKING_NUMBER, it.text.toString())
        }
        addTracking_date?.let {
            outState.putString(FIELD_ORDER_TRACKING_DATE_SHIPPED, it.text.toString())
        }
        addTracking_editCarrier?.let {
            outState.putString(FIELD_ORDER_TRACKING_PROVIDER, it.text.toString())
        }
        addTracking_custom_provider_name?.let {
            outState.putString(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_NAME, it.text.toString())
        }
        addTracking_custom_provider_url?.let {
            outState.putString(FIELD_ORDER_TRACKING_CUSTOM_PROVIDER_URL, it.text.toString())
        }
    }

    override fun confirmDiscard() {
        isConfirmingDiscard = true
        WooDialog.showDialog(
            requireActivity(),
            messageId = R.string.discard_message,
            positiveButtonId = R.string.discard,
            posBtnAction = DialogInterface.OnClickListener { _, _ ->
                shouldShowDiscardDialog = false
                activity?.onBackPressed()
            },
            negativeButtonId = R.string.keep_editing,
            negBtnAction = DialogInterface.OnClickListener { _, _ ->
                isConfirmingDiscard = false
            })
    }

    override fun getProviderText(): String {
        return if (isCustomProvider()) {
            addTracking_custom_provider_name.text.toString()
        } else {
            addTracking_editCarrier.text.toString().trim()
        }
    }

    /**
     * Formats the localized date string and returns it in the format of yyyy-MM-dd
     * for use with the API.
     *
     * example: May 9, 2019 -> 2019-05-09
     */
    override fun getDateShippedText(): String {
        val dateSelected = DateUtils().getDateFromLocalizedLongDateString(
            requireActivity(),
            addTracking_date.text.toString()
        )
        return DateUtils().getYearMonthDayStringFromDate(dateSelected)
    }

    override fun onTrackingProviderSelected(selectedCarrierName: String) {
        addTracking_editCarrier.setText(selectedCarrierName)
        addTracking_editCarrier.error = null
        addTracking_editCarrier.isFocusableInTouchMode = false
        addTracking_editCarrier.isFocusable = false
        isSelectedProviderCustom = addTracking_editCarrier.text.toString() ==
            getString(R.string.order_shipment_tracking_custom_provider_section_name)
        // Display custom provider fields only if selectedCarrierName = custom provider
        if (isCustomProvider()) {
            showCustomProviderFields()
        } else {
            hideCustomProviderFields()
        }
    }

    override fun isCustomProvider() = isSelectedProviderCustom

    override fun showOfflineSnack() {
        uiMessageResolver.showOfflineSnack()
    }

    override fun showAddAddShipmentTrackingErrorSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_error).show()
    }

    override fun showAddShipmentTrackingSnack() {
        uiMessageResolver.getSnack(R.string.order_shipment_tracking_added).show()
    }

    private fun displayFormatDateShippedText(dateString: String) {
        context?.let {
            addTracking_date.setText(
                DateUtils().getLocalizedLongDateString(
                    it,
                    dateString
                )
            )
        }
    }

    private fun showCustomProviderFields() {
        addTracking_customNameLayout.visibility = View.VISIBLE
        addTracking_customUrlLayout.visibility = View.VISIBLE
    }

    private fun hideCustomProviderFields() {
        addTracking_customNameLayout.visibility = View.GONE
        addTracking_customUrlLayout.visibility = View.GONE
    }
}
