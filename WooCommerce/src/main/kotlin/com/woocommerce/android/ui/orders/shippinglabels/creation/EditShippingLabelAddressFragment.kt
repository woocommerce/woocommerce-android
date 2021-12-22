package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
import com.woocommerce.android.ui.common.InputField
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.DialPhoneNumber
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.OpenMapWithAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCountrySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowStateSelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressViewModel.Field
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressSuggestionFragment.Companion.SELECTED_ADDRESS_ACCEPTED
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressSuggestionFragment.Companion.SELECTED_ADDRESS_TO_BE_EDITED
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import com.woocommerce.android.widgets.WCMaterialOutlinedSpinnerView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class EditShippingLabelAddressFragment :
    BaseFragment(R.layout.fragment_edit_shipping_label_address),
    BackPressListener {
    companion object {
        const val SELECT_COUNTRY_REQUEST = "select_country_request"
        const val SELECT_STATE_REQUEST = "select_state_request"
        const val EDIT_ADDRESS_RESULT = "key_edit_address_dialog_result"
        const val EDIT_ADDRESS_CLOSED = "key_edit_address_dialog_closed"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var progressDialog: CustomProgressDialog? = null

    val viewModel: EditShippingLabelAddressViewModel by viewModels()

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentEditShippingLabelAddressBinding.bind(view)

        initializeViewModel(binding)
        initializeViews(binding)
    }

    private fun initializeViewModel(binding: FragmentEditShippingLabelAddressBinding) {
        observeViewState(binding)
        observeEvents()
        setupResultHandlers()
    }

    private fun setupResultHandlers() {
        handleResult<String>(SELECT_COUNTRY_REQUEST) {
            viewModel.onCountrySelected(it)
        }
        handleResult<String>(SELECT_STATE_REQUEST) {
            viewModel.onFieldEdited(Field.State, it)
        }
        handleResult<Address>(SELECTED_ADDRESS_ACCEPTED) {
            viewModel.onAddressSelected(it)
        }
        handleResult<Address>(SELECTED_ADDRESS_TO_BE_EDITED) {
            viewModel.onEditRequested(it)
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
                viewModel.onDoneButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("LongMethod")
    private fun observeViewState(binding: FragmentEditShippingLabelAddressBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.nameField.takeIfNotEqualTo(old?.nameField) { field ->
                binding.name.updateFromField(field)
            }
            new.companyField.takeIfNotEqualTo(old?.nameField) { field ->
                binding.company.updateFromField(field)
            }
            new.address1Field.takeIfNotEqualTo(old?.address1Field) { field ->
                binding.address1.updateFromField(field)
            }
            new.address2Field.takeIfNotEqualTo(old?.address2Field) { field ->
                binding.address2.updateFromField(field)
            }
            new.phoneField.takeIfNotEqualTo(old?.phoneField) { field ->
                binding.phone.updateFromField(field)
            }
            new.cityField.takeIfNotEqualTo(old?.cityField) { field ->
                binding.city.updateFromField(field)
            }
            new.zipField.takeIfNotEqualTo(old?.zipField) { field ->
                binding.zip.updateFromField(field)
            }
            new.stateField.takeIfNotEqualTo(old?.stateField) { field ->
                if (new.isStateFieldSpinner == true) {
                    binding.stateSpinner.setText(field.content)
                    binding.stateSpinner.error = field.error?.let { UiHelpers.getTextOfUiString(requireContext(), it) }
                } else {
                    binding.state.updateFromField(field)
                }
            }
            new.countryField.takeIfNotEqualTo(old?.countryField) { field ->
                binding.countrySpinner.setText(field.content)
                binding.countrySpinner.error = field.error?.let { UiHelpers.getTextOfUiString(requireContext(), it) }
            }
            new.title?.takeIfNotEqualTo(old?.title) {
                screenTitle = getString(it)
            }
            new.bannerMessage?.takeIfNotEqualTo(old?.bannerMessage) {
                if (it.isBlank()) {
                    binding.errorBanner.hide()
                } else {
                    binding.errorBannerMessage.text = it
                    binding.errorBanner.show()
                }
            }
            new.isValidationProgressDialogVisible
                ?.takeIfNotEqualTo(old?.isValidationProgressDialogVisible) { isVisible ->
                    if (isVisible) {
                        showProgressDialog(
                            getString(R.string.shipping_label_edit_address_validation_progress_title),
                            getString(R.string.shipping_label_edit_address_progress_message)
                        )
                    } else {
                        hideProgressDialog()
                    }
                }
            new.isLoadingProgressDialogVisible?.takeIfNotEqualTo(old?.isLoadingProgressDialogVisible) { isVisible ->
                if (isVisible) {
                    showProgressDialog(
                        getString(R.string.shipping_label_edit_address_loading_progress_title),
                        getString(R.string.shipping_label_edit_address_progress_message)
                    )
                } else {
                    hideProgressDialog()
                }
            }
            new.isStateFieldSpinner?.takeIfNotEqualTo(old?.isStateFieldSpinner) { isSpinner ->
                binding.stateSpinner.isVisible = isSpinner
                binding.state.isVisible = !isSpinner
            }
            new.isContactCustomerButtonVisible.takeIfNotEqualTo(old?.isContactCustomerButtonVisible) { isVisible ->
                binding.contactCustomerButton.isVisible = isVisible
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(EDIT_ADDRESS_RESULT, event.data)
                is Exit -> navigateBackWithNotice(EDIT_ADDRESS_CLOSED)
                is ShowSuggestedAddress -> {
                    val action = EditShippingLabelAddressFragmentDirections
                        .actionEditShippingLabelAddressFragmentToShippingLabelAddressSuggestionFragment(
                            event.originalAddress,
                            event.suggestedAddress,
                            event.type
                        )
                    findNavController().navigateSafely(action)
                }
                is ShowCountrySelector -> {
                    val action = EditShippingLabelAddressFragmentDirections.actionSearchFilterFragment(
                        items = event.locations.map {
                            SearchFilterItem(
                                name = it.name,
                                value = it.code
                            )
                        }.toTypedArray(),
                        hint = getString(R.string.shipping_label_edit_address_country_search_hint),
                        requestKey = SELECT_COUNTRY_REQUEST,
                        title = getString(R.string.shipping_label_edit_address_country)
                    )
                    findNavController().navigateSafely(action)
                }
                is ShowStateSelector -> {
                    val action = EditShippingLabelAddressFragmentDirections.actionSearchFilterFragment(
                        items = event.locations.map {
                            SearchFilterItem(
                                name = it.name,
                                value = it.code
                            )
                        }.toTypedArray(),
                        hint = getString(R.string.shipping_label_edit_address_state_search_hint),
                        requestKey = SELECT_STATE_REQUEST,
                        title = getString(R.string.shipping_label_edit_address_state)
                    )
                    findNavController().navigateSafely(action)
                }
                is OpenMapWithAddress -> launchMapsWithAddress(event.address)
                is DialPhoneNumber -> dialPhoneNumber(event.phoneNumber)
                else -> event.isHandled = false
            }
        }
    }

    private fun WCMaterialOutlinedEditTextView.updateFromField(field: InputField<*>) {
        setTextIfDifferent(field.content)
        error = field.error?.let { UiHelpers.getTextOfUiString(requireContext(), it) }
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

    private fun launchMapsWithAddress(address: Address) {
        val cleanAddress = address.copy(
            firstName = "",
            lastName = "",
            phone = "",
            email = ""
        ).toString().replace("\n", ", ")
        val gmmIntentUri: Uri = Uri.parse("geo:0,0?q=$cleanAddress")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_gmaps_app)
        }
    }

    private fun dialPhoneNumber(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_phone_app)
        }
    }

    private fun initializeViews(binding: FragmentEditShippingLabelAddressBinding) {
        binding.name.bindToField(Field.Name)
        binding.company.bindToField(Field.Company)
        binding.address1.bindToField(Field.Address1)
        binding.address2.bindToField(Field.Address2)
        binding.phone.bindToField(Field.Phone)
        binding.city.bindToField(Field.City)
        binding.zip.bindToField(Field.Zip)
        binding.state.bindToField(Field.State)
        binding.useAddressAsIsButton.onClick {
            viewModel.onUseAddressAsIsButtonClicked()
        }
        binding.countrySpinner.onClick {
            viewModel.onCountrySpinnerTapped()
        }
        binding.stateSpinner.onClick {
            viewModel.onStateSpinnerTapped()
        }
        binding.openMapButton.onClick {
            viewModel.onOpenMapTapped()
        }
        binding.contactCustomerButton.onClick {
            viewModel.onContactCustomerTapped()
        }
    }

    private fun WCMaterialOutlinedEditTextView.bindToField(field: Field) {
        setOnTextChangedListener {
            // trigger event only if this view is visible, avoids issues when the field can have different field types
            // like the state
            if (this.isVisible) viewModel.onFieldEdited(field, it?.toString().orEmpty())
        }
    }

    private fun WCMaterialOutlinedSpinnerView.onClick(onClick: () -> Unit) {
        this.setClickListener {
            onClick()
        }
    }

    private fun Button.onClick(onButtonClick: () -> Unit) {
        setOnClickListener {
            onButtonClick()
        }
    }

    // Let the ViewModel know the user is attempting to close the screen
    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
