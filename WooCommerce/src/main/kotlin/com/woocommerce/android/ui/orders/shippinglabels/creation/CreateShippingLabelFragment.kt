package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.CreateShippingLabelEvent.ShowAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.Step
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.CARRIER
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.CUSTOMS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.ORIGIN_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.PACKAGING
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.PAYMENT
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.SHIPPING_ADDRESS
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_create_shipping_label.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
class CreateShippingLabelFragment : BaseFragment() {
    companion object {
        const val KEY_EDIT_ADDRESS_DIALOG_RESULT = "key_edit_address_dialog_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    val viewModel: CreateShippingLabelViewModel by viewModels { viewModelFactory }

    override fun getFragmentTitle() = getString(R.string.shipping_label_create_title)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_shipping_label, container, false)
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
        handleResult<Address>(KEY_EDIT_ADDRESS_DIALOG_RESULT) {
            viewModel.onAddressEditFinished(it)
        }
    }

    private fun subscribeObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.originAddressStep?.takeIfNotEqualTo(old?.originAddressStep) {
                originStep.update(it)
            }
            new.shippingAddressStep?.takeIfNotEqualTo(old?.shippingAddressStep) {
                shippingStep.update(it)
            }
            new.packagingDetailsStep?.takeIfNotEqualTo(old?.packagingDetailsStep) {
                packagingStep.update(it)
            }
            new.customsStep?.takeIfNotEqualTo(old?.customsStep) {
                customsStep.update(it)
            }
            new.carrierStep?.takeIfNotEqualTo(old?.carrierStep) {
                carrierStep.update(it)
            }
            new.paymentStep?.takeIfNotEqualTo(old?.paymentStep) {
                paymentStep.update(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowAddressEditor -> {
                    val action = CreateShippingLabelFragmentDirections
                        .actionCreateShippingLabelFragmentToEditShippingLabelAddressFragment(event.address, event.type)
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun initializeViews() {
        originStep.continueButtonClickListener = { viewModel.onContinueButtonTapped(ORIGIN_ADDRESS) }
        shippingStep.continueButtonClickListener = { viewModel.onContinueButtonTapped(SHIPPING_ADDRESS) }
        packagingStep.continueButtonClickListener = { viewModel.onContinueButtonTapped(PACKAGING) }
        customsStep.continueButtonClickListener = { viewModel.onContinueButtonTapped(CUSTOMS) }
        carrierStep.continueButtonClickListener = { viewModel.onContinueButtonTapped(CARRIER) }
        paymentStep.continueButtonClickListener = { viewModel.onContinueButtonTapped(PAYMENT) }

        originStep.editButtonClickListener = { viewModel.onEditButtonTapped(ORIGIN_ADDRESS) }
        shippingStep.editButtonClickListener = { viewModel.onEditButtonTapped(SHIPPING_ADDRESS) }
        packagingStep.editButtonClickListener = { viewModel.onEditButtonTapped(PACKAGING) }
        customsStep.editButtonClickListener = { viewModel.onEditButtonTapped(CUSTOMS) }
        carrierStep.editButtonClickListener = { viewModel.onEditButtonTapped(CARRIER) }
        paymentStep.editButtonClickListener = { viewModel.onEditButtonTapped(PAYMENT) }
    }

    private fun ShippingLabelCreationStepView.update(data: Step) {
        data.details?.let { details = it }
        data.isEnabled?.let { isViewEnabled = it }
        data.isContinueButtonVisible?.let { isContinueButtonVisible = it }
        data.isEditButtonVisible?.let { isEditButtonVisible = it }
        data.isHighlighted?.let { isHighlighted = it }
    }
}
