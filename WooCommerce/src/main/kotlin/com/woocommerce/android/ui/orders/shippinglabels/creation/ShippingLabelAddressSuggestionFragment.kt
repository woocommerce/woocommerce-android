package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentShippingLabelAddressSuggestionBinding
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.setHtmlText
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.EditSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.UseSelectedAddress
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShippingLabelAddressSuggestionFragment :
    BaseFragment(
        R.layout.fragment_shipping_label_address_suggestion
    ),
    BackPressListener {
    companion object {
        const val SUGGESTED_ADDRESS_DISCARDED = "key_suggested_address_dialog_closed"
        const val SELECTED_ADDRESS_ACCEPTED = "key_selected_address_accepted"
        const val SELECTED_ADDRESS_TO_BE_EDITED = "key_selected_address_to_be_edited"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var _binding: FragmentShippingLabelAddressSuggestionBinding? = null
    private val binding get() = _binding!!

    val viewModel: ShippingLabelAddressSuggestionViewModel by viewModels()

    private var screenTitle = 0
        set(value) {
            field = value
            updateActivityTitle()
        }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShippingLabelAddressSuggestionBinding.bind(view)

        initializeViewModel()
        initializeViews()
    }

    private fun initializeViewModel() {
        subscribeObservers()
    }

    override fun getFragmentTitle() = getString(screenTitle)

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.enteredAddress?.takeIfNotEqualTo(old?.enteredAddress) {
                binding.enteredAddressText.text = it.toString()
            }
            new.suggestedAddress?.takeIfNotEqualTo(old?.suggestedAddress) {
                binding.suggestedAddressText.setHtmlText(it.toStringMarkingDifferences(new.enteredAddress))
            }
            new.selectedAddress?.takeIfNotEqualTo(old?.selectedAddress) { address ->
                binding.suggestedAddressOption.isChecked = new.suggestedAddress == address
                binding.enteredAddressOption.isChecked = new.enteredAddress == address
            }
            new.areButtonsEnabled.takeIfNotEqualTo(old?.areButtonsEnabled) {
                binding.editAddressButton.isEnabled = it
                binding.useSuggestedAddressButton.isEnabled = it
            }
            new.title?.takeIfNotEqualTo(old?.title) {
                screenTitle = it
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(SELECTED_ADDRESS_ACCEPTED, event.data)
                is Exit -> navigateBackWithNotice(SUGGESTED_ADDRESS_DISCARDED)
                is EditSelectedAddress -> navigateBackWithResult(SELECTED_ADDRESS_TO_BE_EDITED, event.address)
                is UseSelectedAddress -> navigateBackWithResult(SELECTED_ADDRESS_ACCEPTED, event.address)
                else -> event.isHandled = false
            }
        }
    }

    private fun Address.toStringMarkingDifferences(other: Address?): String {
        if (other == null) {
            return this.toString()
        }

        val stringBuilder = StringBuilder().appendWithIfNotEmpty(company)

        fun append(thisLine: String, otherLine: String, separator: String = "<br>") {
            if (thisLine.isNotEmpty()) {
                if (thisLine != otherLine) {
                    stringBuilder.append(separator, "<b>", thisLine, "</b>")
                } else {
                    stringBuilder.append(separator, thisLine)
                }
            }
        }

        stringBuilder.appendWithIfNotEmpty("$firstName $lastName".trim(), "<br>")
        append(this.address1, other.address1)
        append(this.address2, other.address2)
        append(this.city, other.city)
        append(this.state.asLocation().name, other.state.asLocation().name, ", ")
        append(this.postcode, other.postcode, " ")
        append(this.country.name, other.country.name)

        return stringBuilder.toString()
    }

    private fun initializeViews() {
        binding.useSuggestedAddressButton.setOnClickListener {
            viewModel.onUseSelectedAddressTapped()
        }
        binding.editAddressButton.setOnClickListener {
            viewModel.onEditSelectedAddressTapped()
        }
        binding.enteredAddressOption.setOnClickListener {
            viewModel.onSelectedAddressChanged(false)
        }
        binding.suggestedAddressOption.setOnClickListener {
            viewModel.onSelectedAddressChanged(true)
        }
        binding.enteredAddressText.setOnClickListener {
            binding.enteredAddressOption.performClick()
        }
        binding.suggestedAddressText.setOnClickListener {
            binding.suggestedAddressOption.performClick()
        }
    }

    // Let the ViewModel know the user is attempting to close the screen
    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
