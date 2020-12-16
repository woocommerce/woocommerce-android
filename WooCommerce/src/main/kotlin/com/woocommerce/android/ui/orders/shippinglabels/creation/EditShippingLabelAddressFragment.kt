package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditShippingLabelAddressBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.CancelAddressEditing
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCountrySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@ExperimentalCoroutinesApi
class EditShippingLabelAddressFragment : BaseFragment(), BackPressListener {
    companion object {
        const val SELECT_COUNTRY_REQUEST = "select_country_request"
        const val SELECT_STATE_REQUEST = "select_state_request"
    }
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private var progressDialog: CustomProgressDialog? = null
    private var _binding: FragmentEditShippingLabelAddressBinding? = null
    private val binding get() = _binding!!

    val viewModel: EditShippingLabelAddressViewModel by viewModels { viewModelFactory }

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)

        _binding = FragmentEditShippingLabelAddressBinding.inflate(inflater, container, false)
        return binding.root
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        initializeViews()
    }

    private fun initializeViewModel() {
        subscribeObservers()
        setupResultHandlers()
    }

    private fun setupResultHandlers() {
        handleResult<String>(SELECT_COUNTRY_REQUEST) {
            viewModel.onCountrySelected(it)
        }
        handleResult<String>(SELECT_STATE_REQUEST) {
        }
    }

    override fun getFragmentTitle() = screenTitle

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(gatherData())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.address?.takeIfNotEqualTo(old?.address) {
                binding.company.setText(it.company)
                binding.name.setText("${it.firstName} ${it.lastName}")
                binding.phone.setText(it.phone)
                binding.address1.setText(it.address1)
                binding.address2.setText(it.address2)
                binding.zip.setText(it.postcode)
                binding.state.setText(it.state)
                binding.stateSpinner.setText(it.state)
                binding.city.setText(it.city)
                binding.countrySpinner.tag = it.country
            }
            new.title?.takeIfNotEqualTo(old?.title) {
                screenTitle = getString(it)
            }
            new.addressError?.takeIfNotEqualTo(old?.addressError) {
                showErrorOrClear(binding.address1Layout, it)
            }
            new.nameError?.takeIfNotEqualTo(old?.nameError) {
                showErrorOrClear(binding.nameLayout, it)
            }
            new.cityError?.takeIfNotEqualTo(old?.cityError) {
                showErrorOrClear(binding.cityLayout, it)
            }
            new.zipError?.takeIfNotEqualTo(old?.zipError) {
                showErrorOrClear(binding.zipLayout, it)
            }
            new.bannerMessage?.takeIfNotEqualTo(old?.bannerMessage) {
                if (it.isBlank()) {
                    binding.errorBanner.hide()
                } else {
                    binding.errorBannerMessage.text = it
                    binding.errorBanner.show()
                }
            }
            new.isProgressDialogVisible?.takeIfNotEqualTo(old?.isProgressDialogVisible) { isVisible ->
                if (isVisible) {
                    showProgressDialog()
                } else {
                    hideProgressDialog()
                }
            }
            new.selectedCountryName?.takeIfNotEqualTo(old?.selectedCountryName) {
                binding.countrySpinner.setText(it)
            }
            new.isStateFieldSpinner?.takeIfNotEqualTo(old?.isStateFieldSpinner) { isSpinner ->
                binding.stateSpinner.isVisible = isSpinner
                binding.stateLayout.isVisible = !isSpinner
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(
                    CreateShippingLabelFragment.EDIT_ADDRESS_RESULT,
                    event.data
                )
                is CancelAddressEditing -> navigateBackWithNotice(
                    CreateShippingLabelFragment.EDIT_ADDRESS_CLOSED
                )
                is Exit -> findNavController().navigateUp()
                is ShowSuggestedAddress -> {
                }
                is ShowCountrySelector -> {
                    val action = EditShippingLabelAddressFragmentDirections
                        .actionEditShippingLabelAddressFragmentToItemSelectorDialog(
                            event.currentCountry,
                            event.locations.map { it.name }.toTypedArray(),
                            event.locations.map { it.code }.toTypedArray(),
                            SELECT_COUNTRY_REQUEST,
                            getString(R.string.shipping_label_edit_address_country)
                        )
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun showErrorOrClear(inputLayout: TextInputLayout, @StringRes message: Int) {
        if (message == 0) {
            inputLayout.error = null
        } else {
            inputLayout.error = resources.getString(message)
        }
    }

    private fun showProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            title = getString(R.string.shipping_label_edit_address_validation_progress_title),
            message = getString(R.string.shipping_label_edit_address_validation_progress_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun initializeViews() {
        binding.btnUsAddressAsIs.setOnClickListener {
            viewModel.onUseAddressAsIsButtonClicked(gatherData())
        }
        binding.countrySpinner.setClickListener {
            viewModel.onCountrySpinnerTapped()
        }
    }

    private fun gatherData(): Address {
        return Address(
            company = binding.company.text.toString(),
            firstName = binding.name.text.toString(),
            lastName = "",
            phone = binding.phone.text.toString(),
            address1 = binding.address1.text.toString(),
            address2 = binding.address2.text.toString(),
            postcode = binding.zip.text.toString(),
            state = binding.state.text.toString(),
            city = binding.city.text.toString(),
            country = binding.countrySpinner.tag as String,
            email = ""
        )
    }

    // Let the ViewModel know the user is attempting to close the screen
    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
