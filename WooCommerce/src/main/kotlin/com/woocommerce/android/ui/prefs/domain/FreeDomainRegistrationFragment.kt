@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.prefs.domain

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import com.woocommerce.android.databinding.FreeDomainRegistrationFragmentBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.DomainContactFormModel
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.NavigateToDomainDashboard
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.NavigateToPurchaseSuccessScreen
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.ShowCountryPickerDialog
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.ShowErrorMessage
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.ShowFormValidationError
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.ShowStatePickerDialog
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.ShowTermsOfService
import com.woocommerce.android.ui.prefs.domain.FreeDomainRegistrationViewModel.ViewState
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.ADDRESS_1
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.ADDRESS_2
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.CITY
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.COUNTRY_CODE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.EMAIL
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.FIRST_NAME
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.LAST_NAME
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.ORGANIZATION
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.PHONE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.POSTAL_CODE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.STATE
import org.wordpress.android.util.LanguageUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class FreeDomainRegistrationFragment : BaseFragment() {
    companion object {
        private const val EXTRA_DOMAIN_PRODUCT_DETAILS = "EXTRA_DOMAIN_PRODUCT_DETAILS"
        private const val URL_TOS = "https://wordpress.com/tos"
        const val TAG = "DOMAIN_REGISTRATION_DETAILS"

        fun newInstance(domainProductDetails: DomainProductDetails): FreeDomainRegistrationFragment {
            val fragment = FreeDomainRegistrationFragment()
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_DOMAIN_PRODUCT_DETAILS, domainProductDetails)
            fragment.arguments = bundle
            return fragment
        }
    }

    private val viewModel: FreeDomainRegistrationViewModel by viewModels()

    @Suppress("DEPRECATION")
    private var loadingProgressDialog: ProgressDialog? = null
    private var binding: FreeDomainRegistrationFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.free_domain_registration_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FreeDomainRegistrationFragmentBinding.bind(view)) {
            binding = this
            setupObservers()

            domainPrivacyOnRadioButton.setOnClickListener {
                viewModel.togglePrivacyProtection(true)
            }

            domainPrivacyOffRadioButton.setOnClickListener {
                viewModel.togglePrivacyProtection(false)
            }

            // Country and State input could only be populated from the dialog
            countryInput.inputType = 0
            countryInput.setOnClickListener {
                viewModel.onCountrySelectorClicked()
            }

            stateInput.inputType = 0
            stateInput.setOnClickListener {
                viewModel.onStateSelectorClicked()
            }

            registerDomainButton.setOnClickListener {
                if (validateForm()) {
                    viewModel.onRegisterDomainButtonClicked()
                }
            }

            setupTosLink()
            setupInputFieldTextWatchers()
            setupEventObservers()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setupEventObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is ShowErrorMessage -> ToastUtils.showToast(context, event.message)
                is ShowFormValidationError -> binding?.handleFormValidationError(event.error)
                is ShowCountryPickerDialog -> showCountryPicker(event.countries)
                is ShowStatePickerDialog -> showStatePicker(event.states)
                is NavigateToPurchaseSuccessScreen -> navigateToPurchaseSuccessScreen(event.domain)
                is NavigateToDomainDashboard -> navigateToDomainDashboard(event.source)
                ShowTermsOfService -> showTermsOfService()
            }
        }
    }

    private fun navigateToPurchaseSuccessScreen(domain: String) {
        findNavController().navigateSafely(
            FreeDomainRegistrationFragmentDirections
                .actionDomainRegistrationDetailsFragmentToPurchaseSuccessfulFragment(domain)
        )
    }

    private fun navigateToDomainDashboard(source: DomainFlowSource) {
        findNavController().navigateSafely(
            FreeDomainRegistrationFragmentDirections
                .actionDomainRegistrationDetailsFragmentToDomainDashboardFragment(source = source)
        )
    }

    @Suppress("EmptyFunctionBlock")
    private fun FreeDomainRegistrationFragmentBinding.setupInputFieldTextWatchers() {
        arrayOf(
            firstNameInput,
            lastNameInput,
            organizationInput,
            emailInput,
            countryCodeInput,
            phoneNumberInput,
            addressFirstLineInput,
            addressSecondLineInput,
            cityInput,
            postalCodeInput
        ).forEach {
            it.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    viewModel.onDomainContactDetailsChanged(getDomainContactFormModel())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }
    }

    // make link to ToS clickable
    private fun FreeDomainRegistrationFragmentBinding.setupTosLink() {
        tosExplanation.text = HtmlCompat.fromHtml(
            String.format(
                resources.getString(R.string.domain_registration_privacy_protection_tos),
                "<u>",
                "</u>"
            ),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        tosExplanation.movementMethod = LinkMovementMethod.getInstance()
        tosExplanation.setOnClickListener {
            viewModel.onTosLinkClicked()
        }
    }

    private fun FreeDomainRegistrationFragmentBinding.setupObservers() {
        viewModel.viewState.observe(viewLifecycleOwner) {
            it?.let { uiState -> loadState(uiState) }
        }

        viewModel.domainContactForm.observe(viewLifecycleOwner) { domainContactFormModel ->
            val currentModel = getDomainContactFormModel()
            if (currentModel != domainContactFormModel) {
                populateContactForm(domainContactFormModel!!)
            }
        }
    }

    private fun showTermsOfService() {
        ChromeCustomTabUtils.launchUrl(
            context = requireContext(),
            url = URL_TOS + "?locale=" + LanguageUtils.getPatchedCurrentDeviceLanguage(requireContext())
        )
    }

    private fun FreeDomainRegistrationFragmentBinding.loadState(uiState: ViewState) {
        toggleFormProgressIndicator(uiState.isFormProgressIndicatorVisible)
        toggleStateProgressIndicator(uiState.isStateProgressIndicatorVisible)
        toggleStateInputEnabledState(uiState.isStateInputEnabled)

        if (uiState.isRegistrationProgressIndicatorVisible) {
            showDomainRegistrationProgressDialog()
        } else {
            hideDomainRegistrationProgressDialog()
        }

        if (uiState.isPrivacyProtectionEnabled) {
            domainPrivacyOptionsRadiogroup.check(R.id.domain_privacy_on_radio_button)
        } else {
            domainPrivacyOptionsRadiogroup.check(R.id.domain_privacy_off_radio_button)
        }

        registerDomainButton.isEnabled = uiState.isDomainRegistrationButtonEnabled

        // Country and State fields treated as UI state, since we only use them for display purpose
        countryInput.setText(uiState.selectedCountry?.name)
        stateInput.setText(uiState.selectedState?.name)
    }

    private fun FreeDomainRegistrationFragmentBinding.handleFormValidationError(error: RedeemShoppingCartError) {
        var affectedInputFields: Array<TextInputEditText>? = null

        when (error.type) {
            FIRST_NAME -> affectedInputFields = arrayOf(firstNameInput)
            LAST_NAME -> affectedInputFields = arrayOf(lastNameInput)
            ORGANIZATION -> affectedInputFields = arrayOf(organizationInput)
            ADDRESS_1 -> affectedInputFields = arrayOf(addressFirstLineInput)
            ADDRESS_2 -> affectedInputFields = arrayOf(addressSecondLineInput)
            POSTAL_CODE -> affectedInputFields = arrayOf(postalCodeInput)
            CITY -> affectedInputFields = arrayOf(cityInput)
            STATE -> affectedInputFields = arrayOf(stateInput)
            COUNTRY_CODE -> affectedInputFields = arrayOf(countryInput)
            EMAIL -> affectedInputFields = arrayOf(emailInput)
            PHONE -> affectedInputFields = arrayOf(
                countryCodeInput,
                phoneNumberInput
            )
            else -> {
            } // Something else, will just show a Toast with an error message
        }
        affectedInputFields?.forEach {
            @Suppress("DEPRECATION")
            showFieldError(it, StringEscapeUtils.unescapeHtml4(error.message))
        }
        affectedInputFields?.firstOrNull { it.requestFocus() }
    }

    private fun FreeDomainRegistrationFragmentBinding.populateContactForm(formModel: DomainContactFormModel) {
        firstNameInput.setText(formModel.firstName)
        lastNameInput.setText(formModel.lastName)
        organizationInput.setText(formModel.organization)
        emailInput.setText(formModel.email)
        countryCodeInput.setText(formModel.phoneNumberPrefix)
        phoneNumberInput.setText(formModel.phoneNumber)
        addressFirstLineInput.setText(formModel.addressLine1)
        addressSecondLineInput.setText(formModel.addressLine2)
        cityInput.setText(formModel.city)
        postalCodeInput.setText(formModel.postalCode)
    }

    // local validation
    private fun FreeDomainRegistrationFragmentBinding.validateForm(): Boolean {
        var formIsCompleted = true

        val requiredFields = arrayOf(
            firstNameInput,
            lastNameInput,
            emailInput,
            countryCodeInput,
            phoneNumberInput,
            countryInput,
            addressFirstLineInput,
            cityInput,
            postalCodeInput
        )

        var fieldToFocusOn: TextInputEditText? = null

        requiredFields.forEach {
            clearEmptyFieldError(it)

            if (TextUtils.isEmpty(it.text)) {
                if (fieldToFocusOn == null) {
                    fieldToFocusOn = it
                }
                showEmptyFieldError(it)
                if (formIsCompleted) {
                    formIsCompleted = false
                }
            }
        }

        // focusing on first empty field
        fieldToFocusOn?.requestFocus()

        return formIsCompleted
    }

    private fun showEmptyFieldError(editText: EditText) {
        val parent = editText.parent.parent
        if (parent is TextInputLayout) {
            showFieldError(
                editText,
                getString(R.string.domain_registration_contact_form_input_error, parent.hint)
            )
        }
    }

    private fun clearEmptyFieldError(editText: EditText) {
        val parent = editText.parent.parent
        if (parent is TextInputLayout) {
            showFieldError(editText, null)
        }
    }

    private fun showFieldError(editText: EditText, errorMessage: String?) {
        editText.error = errorMessage
    }

    private fun getDomainContactFormModel(): DomainContactFormModel = with(binding!!) {
        return DomainContactFormModel(
            firstName = firstNameInput.text.toString(),
            lastName = lastNameInput.text.toString(),
            organization = StringUtils.notNullStr(organizationInput.text.toString()),
            addressLine1 = addressFirstLineInput.text.toString(),
            addressLine2 = addressSecondLineInput.text.toString(),
            postalCode = postalCodeInput.text.toString(),
            city = cityInput.text.toString(),
            state = null, // state code will be added in ViewModel
            countryCode = null, // country code will be added in ViewModel
            email = emailInput.text.toString(),
            phoneNumberPrefix = countryCodeInput.text.toString(),
            phoneNumber = phoneNumberInput.text.toString()
        )
    }

    @Suppress("DEPRECATION")
    private fun showStatePicker(states: List<SupportedStateResponse>) {
        val dialogFragment = StatePickerDialogFragment.newInstance(states.toCollection(ArrayList()), viewModel)
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(requireFragmentManager(), StatePickerDialogFragment.TAG)
    }

    @Suppress("DEPRECATION")
    private fun showCountryPicker(countries: List<SupportedDomainCountry>) {
        val dialogFragment = CountryPickerDialogFragment.newInstance(countries.toCollection(ArrayList()), viewModel)
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(requireFragmentManager(), CountryPickerDialogFragment.TAG)
    }

    private fun FreeDomainRegistrationFragmentBinding.toggleFormProgressIndicator(visible: Boolean) {
        if (visible) {
            formProgressIndicator.visibility = View.VISIBLE
        } else {
            formProgressIndicator.visibility = View.GONE
        }
    }

    private fun FreeDomainRegistrationFragmentBinding.toggleStateProgressIndicator(visible: Boolean) {
        if (visible) {
            statesLoadingProgressIndicator.visibility = View.VISIBLE
        } else {
            statesLoadingProgressIndicator.visibility = View.GONE
        }

        stateInputContainer.isEnabled = !visible
    }

    private fun FreeDomainRegistrationFragmentBinding.toggleStateInputEnabledState(enabled: Boolean) {
        stateInputContainer.isEnabled = enabled
        if (enabled) {
            stateInputContainer.hint = getString(R.string.domain_contact_information_state_hint)
        } else {
            stateInputContainer.hint = getString(R.string.domain_contact_information_state_not_available_hint)
        }
    }

    @Suppress("DEPRECATION")
    private fun showDomainRegistrationProgressDialog() {
        if (loadingProgressDialog == null) {
            loadingProgressDialog = ProgressDialog(context)
            loadingProgressDialog!!.isIndeterminate = true
            loadingProgressDialog!!.setCancelable(false)

            loadingProgressDialog!!
                .setMessage(getString(R.string.domain_registration_registering_domain_name_progress_dialog_message))
        }
        if (!loadingProgressDialog!!.isShowing) {
            loadingProgressDialog!!.show()
        }
    }

    private fun hideDomainRegistrationProgressDialog() {
        if (loadingProgressDialog != null && loadingProgressDialog!!.isShowing) {
            loadingProgressDialog!!.cancel()
        }
    }

    @AndroidEntryPoint
    internal class StatePickerDialogFragment : DialogFragment() {
        private lateinit var states: List<SupportedStateResponse>

        private lateinit var viewModel: FreeDomainRegistrationViewModel

        companion object {
            private const val EXTRA_STATES = "EXTRA_STATES"
            const val TAG = "STATE_PICKER_DIALOG_FRAGMENT"

            fun newInstance(
                states: ArrayList<SupportedStateResponse>,
                viewModel: FreeDomainRegistrationViewModel
            ): StatePickerDialogFragment {
                val fragment = StatePickerDialogFragment()
                val bundle = Bundle()
                bundle.putParcelableArrayList(EXTRA_STATES, states)
                fragment.arguments = bundle
                fragment.viewModel = viewModel
                return fragment
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            states = requireArguments().getParcelableArrayList<SupportedStateResponse>(EXTRA_STATES)
                as ArrayList<SupportedStateResponse>
        }

        @Suppress("DEPRECATION", "UseCheckOrError")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if (targetFragment == null) {
                throw IllegalStateException("StatePickerDialogFragment is missing a targetFragment ")
            }

            val builder = MaterialAlertDialogBuilder(ContextThemeWrapper(requireActivity(), style.Theme_Woo_Dialog))
            builder.setTitle(R.string.domain_registration_state_picker_dialog_title)
            builder.setItems(states.map { it.name }.toTypedArray()) { _, which ->
                viewModel.onStateSelected(states[which])
            }

            builder.setPositiveButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }

            return builder.create()
        }
    }

    @AndroidEntryPoint
    internal class CountryPickerDialogFragment : DialogFragment() {
        private lateinit var countries: List<SupportedDomainCountry>

        private lateinit var viewModel: FreeDomainRegistrationViewModel

        companion object {
            private const val EXTRA_COUNTRIES = "EXTRA_COUNTRIES"
            const val TAG = "COUNTRY_PICKER_DIALOG_FRAGMENT"

            fun newInstance(
                countries: ArrayList<SupportedDomainCountry>,
                viewModel: FreeDomainRegistrationViewModel
            ): CountryPickerDialogFragment {
                val fragment = CountryPickerDialogFragment()
                val bundle = Bundle()
                bundle.putParcelableArrayList(EXTRA_COUNTRIES, countries)
                fragment.arguments = bundle
                fragment.viewModel = viewModel
                return fragment
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            countries = arguments?.getParcelableArrayList<SupportedDomainCountry>(EXTRA_COUNTRIES)
                as ArrayList<SupportedDomainCountry>
        }

        @Suppress("DEPRECATION", "UseCheckOrError")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if (targetFragment == null) {
                throw IllegalStateException("CountryPickerDialogFragment is missing a targetFragment ")
            }

            val builder = MaterialAlertDialogBuilder(ContextThemeWrapper(requireActivity(), style.Theme_Woo_Dialog))
            builder.setTitle(R.string.domain_registration_country_picker_dialog_title)
            builder.setItems(countries.map { it.name }.toTypedArray()) { _, which ->
                viewModel.onCountrySelected(countries[which])
            }

            builder.setPositiveButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }

            return builder.create()
        }
    }
}
