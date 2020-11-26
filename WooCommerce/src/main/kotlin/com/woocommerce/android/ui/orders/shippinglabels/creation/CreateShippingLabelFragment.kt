package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.Step
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_create_shipping_label.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
class CreateShippingLabelFragment  : BaseFragment() {
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
        }

        originStep.continueButtonClickListener = viewModel::onValidateOriginButtonTapped
        originStep.editButtonClickListener = viewModel::onEditOriginAddressButtonTapped
        shippingStep.continueButtonClickListener = viewModel::onValidateShippingButtonTapped
        shippingStep.editButtonClickListener = viewModel::onEditShippingAddressButtonTapped
        packagingStep.continueButtonClickListener = viewModel::onContinueToCarrierButtonTapped
        packagingStep.editButtonClickListener = viewModel::onEditPackagingButtonTapped
    }

    private fun ShippingLabelCreationStepView.update(data: Step) {
        details = data.details
        isViewEnabled = data.isEnabled
        isContinueButtonVisible = data.isContinueButtonVisible
        isEditButtonVisible = data.isEditButtonVisible
    }
}
