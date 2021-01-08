package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentShippingLabelAddressSuggestionBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.DiscardSuggestedAddress
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ShippingLabelAddressSuggestionFragment
    : BaseFragment(R.layout.fragment_shipping_label_address_suggestion), BackPressListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private var _binding: FragmentShippingLabelAddressSuggestionBinding? = null
    private val binding get() = _binding!!

    val viewModel: ShippingLabelAddressSuggestionViewModel by viewModels { viewModelFactory }

    private var screenTitle = ""
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

    override fun getFragmentTitle() = screenTitle

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(
                    CreateShippingLabelFragment.SUGGESTED_ADDRESS_SELECTED,
                    event.data
                )
                is DiscardSuggestedAddress -> navigateBackWithNotice(
                    CreateShippingLabelFragment.DISCARD_SUGGESTED_ADDRESS
                )
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    private fun initializeViews() {
        binding.useSuggestedAddressButton.setOnClickListener {
        }
        binding.editAddressButton.setOnClickListener {
        }
        binding.enteredAddressOption.setOnClickListener {
        }
        binding.suggestedAddressOption.setOnClickListener {
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
