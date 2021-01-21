package com.woocommerce.android.ui.products

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductShippingBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.isFloat
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.ui.products.ProductShippingClassFragment.Companion.SELECTED_SHIPPING_CLASS_RESULT
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView

/**
 * Fragment which enables updating product shipping data.
 */
class ProductShippingFragment : BaseProductEditorFragment(R.layout.fragment_product_shipping) {
    private val viewModel: ProductShippingViewModel by viewModels { viewModelFactory.get() }

    override val isDoneButtonVisible: Boolean
        get() = viewModel.viewStateData.liveData.value?.isDoneButtonVisible ?: false
    override val isDoneButtonEnabled: Boolean = true
    override val lastEvent: Event?
        get() = viewModel.event.value

    private var _binding: FragmentProductShippingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductShippingBinding.bind(view)

        setupObservers(viewModel)
        setupResultHandlers()
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_settings)

    private fun setupObservers(viewModel: ProductShippingViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isShippingClassSectionVisible?.takeIfNotEqualTo(old?.isShippingClassSectionVisible) { isVisible ->
                binding.productShippingClassSpinner.isVisible = isVisible
            }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isVisible ->
                doneButton?.isVisible = isVisible
            }
            new.shippingData.weight?.takeIfNotEqualTo(old?.shippingData?.weight) { weight ->
                showValue(binding.productWeight, R.string.product_weight, weight, viewModel.parameters.weightUnit)
            }
            new.shippingData.length?.takeIfNotEqualTo(old?.shippingData?.length) { length ->
                showValue(binding.productLength, R.string.product_length, length, viewModel.parameters.dimensionUnit)
            }
            new.shippingData.width?.takeIfNotEqualTo(old?.shippingData?.width) { width ->
                showValue(binding.productWidth, R.string.product_width, width, viewModel.parameters.dimensionUnit)
            }
            new.shippingData.height?.takeIfNotEqualTo(old?.shippingData?.height) { height ->
                showValue(binding.productHeight, R.string.product_height, height, viewModel.parameters.dimensionUnit)
            }
            new.shippingData.shippingClassId?.takeIfNotEqualTo(old?.shippingData?.shippingClassId) { classId ->
                binding.productShippingClassSpinner.setText(viewModel.getShippingClassByRemoteShippingClassId(classId))
            }
        }
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(KEY_SHIPPING_DIALOG_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                is ShowDialog -> event.showDialog()
                else -> event.isHandled = false
            }
        })
    }

    private fun setupResultHandlers() {
        handleResult<ShippingClass>(SELECTED_SHIPPING_CLASS_RESULT) { shippingClass ->
            viewModel.onDataChanged(
                shippingClassSlug = shippingClass.slug,
                shippingClassId = shippingClass.remoteShippingClassId
            )
        }
    }

    private fun setupViews() {
        binding.productWeight.setOnTextChangedListener {
            viewModel.onDataChanged(weight = editableToFloat(it))
        }
        binding.productLength.setOnTextChangedListener {
            viewModel.onDataChanged(length = editableToFloat(it))
        }
        binding.productHeight.setOnTextChangedListener {
            viewModel.onDataChanged(height = editableToFloat(it))
        }
        binding.productWidth.setOnTextChangedListener {
            viewModel.onDataChanged(width = editableToFloat(it))
        }
        binding.productShippingClassSpinner.setClickListener {
            showShippingClassFragment()
        }
    }

    private fun editableToFloat(editable: Editable?): Float? {
        val str = editable?.toString() ?: ""
        return if (str.isFloat()) {
            str.toFloat()
        } else 0.0f
    }

    /**
     * Shows the passed weight or dimension value in the passed view and sets the hint so it
     * includes the weight or dimension unit, ex: "Width (in)"
     */
    private fun showValue(view: WCMaterialOutlinedEditTextView, @StringRes hintRes: Int, value: Float?, unit: String?) {
        if (value != editableToFloat(view.editText?.text)) {
            val valStr = if (value != 0.0f) (value?.toString() ?: "") else ""
            view.setText(valStr)
        }
        view.hint = if (unit != null) {
            getString(hintRes) + " ($unit)"
        } else {
            getString(hintRes)
        }
    }

    private fun showShippingClassFragment() {
        val action = ProductShippingFragmentDirections
                .actionProductShippingFragmentToProductShippingClassFragment(
                    productShippingClassId = viewModel.shippingData.shippingClassId ?: -1
                )
        findNavController().navigateSafely(action)
    }

    override fun onDoneButtonClicked() {
        viewModel.onDoneButtonClicked()
    }

    override fun onExit() {
        viewModel.onExit()
    }
}
