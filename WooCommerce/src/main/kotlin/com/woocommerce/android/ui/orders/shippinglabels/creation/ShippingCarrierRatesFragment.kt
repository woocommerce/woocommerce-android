package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentShippingCarrierRatesBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ShippingCarrierRatesFragment : BaseFragment(R.layout.fragment_shipping_carrier_rates), BackPressListener {
    companion object {
        const val SHIPPING_CARRIERS_CLOSED = "shipping_carriers_closed"
        const val SHIPPING_CARRIERS_RESULT = "shipping_carriers_result"
    }
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private var progressDialog: CustomProgressDialog? = null
    private var _binding: FragmentShippingCarrierRatesBinding? = null
    private val binding get() = _binding!!

    val viewModel: ShippingCarrierRatesViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShippingCarrierRatesBinding.bind(view)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        subscribeObservers()
    }

    override fun getFragmentTitle() = getString(R.string.shipping_label_shipping_carriers_title)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.shippingRates.observe(viewLifecycleOwner) {

        }

        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.bannerMessage?.takeIfNotEqualTo(old?.bannerMessage) {
            }
            new.isLoadingProgressDialogVisible?.takeIfNotEqualTo(old?.isLoadingProgressDialogVisible) { isVisible ->
                if (isVisible) {
                    showProgressDialog(
                        getString(R.string.shipping_label_edit_address_validation_progress_title),
                        getString(R.string.shipping_label_edit_address_loading_progress_title)
                    )
                } else {
                    hideProgressDialog()
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(SHIPPING_CARRIERS_RESULT, event.data)
                is Exit -> navigateBackWithNotice(SHIPPING_CARRIERS_CLOSED)
                else -> event.isHandled = false
            }
        })
    }

    private fun showProgressDialog(title: String, message: String) {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            title = title,
            message = message
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    // Let the ViewModel know the user is attempting to close the screen
    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
