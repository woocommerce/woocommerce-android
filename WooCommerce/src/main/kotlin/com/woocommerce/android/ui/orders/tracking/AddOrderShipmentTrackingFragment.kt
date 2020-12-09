package com.woocommerce.android.ui.orders.tracking

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddShipmentTrackingBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import org.wordpress.android.util.ActivityUtils
import java.util.Calendar
import javax.inject.Inject
import org.wordpress.android.fluxc.utils.DateUtils as FluxCDateUtils

class AddOrderShipmentTrackingFragment : BaseFragment(R.layout.fragment_add_shipment_tracking),
    AddOrderTrackingProviderActionListener,
    BackPressListener {
    companion object {
        const val KEY_ADD_SHIPMENT_TRACKING_RESULT = "key_add_shipment_tracking_result"
    }

    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: AddOrderShipmentTrackingViewModel by viewModels { viewModelFactory }

    private var isSelectedProviderCustom = false
    private var dateShippedPickerDialog: DatePickerDialog? = null
    private var providerListPickerDialog: AddOrderTrackingProviderListFragment? = null
    private var progressDialog: CustomProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_shipment_tracking, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.order_shipment_tracking_toolbar_title)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAddShipmentTrackingBinding.bind(view)
        initUi(binding)
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentAddShipmentTrackingBinding) {
        viewModel.addOrderShipmentTrackingViewStateData.observe(viewLifecycleOwner) { old, new ->
            //Carrier data
            new.carrier.takeIfNotEqualTo(old?.carrier) {
                if (it.isCustom) {
                    showCustomProviderFields(binding)
                    binding.carrier.setText(getString(R.string.order_shipment_tracking_custom_provider_section_name))
                    if (binding.customProviderName.text.toString() != it.name) {
                        binding.customProviderName.setText(it.name)
                    }
                } else {
                    hideCustomProviderFields(binding)
                    binding.carrier.setText(it.name)
                }
            }
            new.carrierError.takeIfNotEqualTo(old?.carrierError) { error ->
                binding.carrierLayout.error = error?.let { getString(it) }
            }
            new.customCarrierNameError.takeIfNotEqualTo(old?.customCarrierNameError) { error ->
                binding.customProviderNameLayout.error = error?.let { getString(it) }
            }

            //tracking number
            new.trackingNumber.takeIfNotEqualTo(old?.trackingNumber) {
                if (binding.trackingNumber.text.toString() != it) {
                    binding.trackingNumber.setText(it)
                }
            }
            new.trackingNumberError.takeIfNotEqualTo(old?.trackingNumberError) { error ->
                binding.trackingNumberLayout.error = error?.let { getString(it) }
            }

            //custom URL
            new.trackingLink.takeIfNotEqualTo(old?.trackingLink) {
                if (binding.customProviderUrl.text.toString() != it) {
                    binding.customProviderUrl.setText(it)
                }
            }

            new.date.takeIfNotEqualTo(old?.date) {
                binding.date.setText(DateUtils().getLocalizedLongDateString(requireActivity(), it))
            }

            new.showLoadingProgress.takeIfNotEqualTo(old?.showLoadingProgress) {
                showProgressDialog(it)
            }
        }
        viewModel.event.observe(viewLifecycleOwner) {event ->
            when(event){
                is ShowDialog -> event.showDialog()
                is Exit -> findNavController().navigateUp()
                is ExitWithResult<*> -> navigateBackWithResult(KEY_ADD_SHIPMENT_TRACKING_RESULT, event.data)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        }
    }

    private fun initUi(binding: FragmentAddShipmentTrackingBinding) {
        binding.carrier.setOnClickListener {
            providerListPickerDialog = AddOrderTrackingProviderListFragment
                .newInstance(
                    listener = this,
                    selectedProviderText = binding.carrier.text.toString(),
                    orderIdentifier = viewModel.orderId
                )
                .also { it.show(parentFragmentManager, AddOrderTrackingProviderListFragment.TAG) }
        }

        binding.date.setOnClickListener {
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

        binding.customProviderName.doOnTextChanged { text, _, _, _ ->
            viewModel.onCustomCarrierNameEntered(text.toString())
        }
        binding.trackingNumber.doOnTextChanged { text, _, _, _ ->
            viewModel.onTrackingNumberEntered(text.toString())
        }
        binding.customProviderUrl.doOnTextChanged { text, _, _, _ ->
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
                activity?.let {
                    ActivityUtils.hideKeyboard(it)
                }
                viewModel.onAddButtonTapped()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonPressed()
    }

    override fun onTrackingProviderSelected(selectedCarrierName: String) {
        isSelectedProviderCustom = selectedCarrierName ==
            getString(R.string.order_shipment_tracking_custom_provider_section_name)

        val carrier = Carrier(
            name = if (isSelectedProviderCustom) "" else selectedCarrierName,
            isCustom = isSelectedProviderCustom
        )
        viewModel.onCarrierSelected(carrier)
    }

    private fun showCustomProviderFields(binding: FragmentAddShipmentTrackingBinding) {
        binding.customProviderNameLayout.visibility = View.VISIBLE
        binding.customProviderUrlLayout.visibility = View.VISIBLE
    }

    private fun hideCustomProviderFields(binding: FragmentAddShipmentTrackingBinding) {
        binding.customProviderNameLayout.visibility = View.GONE
        binding.customProviderUrlLayout.visibility = View.GONE
    }

    private fun showProgressDialog(show: Boolean) {
        progressDialog?.dismiss()
        if (show) {
            progressDialog = CustomProgressDialog.show(
                getString(R.string.add_order_note_progress_title),
                getString(R.string.add_order_note_progress_message)
            ).also {
                it.show(parentFragmentManager, CustomProgressDialog.TAG)
            }
            progressDialog?.isCancelable = false
        }
    }
}
