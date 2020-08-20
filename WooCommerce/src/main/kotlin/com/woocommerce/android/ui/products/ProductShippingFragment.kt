package com.woocommerce.android.ui.products

import android.os.Bundle
import android.text.Editable
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
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.isFloat
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.ProductShippingClassFragment.Companion.ARG_SELECTED_SHIPPING_CLASS_ID
import com.woocommerce.android.ui.products.ProductShippingClassFragment.Companion.ARG_SELECTED_SHIPPING_CLASS_SLUG
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import kotlinx.android.synthetic.main.fragment_product_shipping.*
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

/**
 * Fragment which enables updating product shipping data.
 */
class ProductShippingFragment : BaseFragment(), BackPressListener, NavigationResult {
    companion object {
        const val KEY_SHIPPING_DIALOG_RESULT = "key_shipping_dialog_result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductShippingViewModel by viewModels { viewModelFactory }

    private var doneButton: MenuItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_shipping, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        setupViews()
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_settings)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
        doneButton = menu.findItem(R.id.menu_done)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val viewState = viewModel.viewStateData.liveData.value
        doneButton?.isVisible = viewState?.isDoneButtonVisible ?: false
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

    private fun setupObservers(viewModel: ProductShippingViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isShippingClassSectionVisible?.takeIfNotEqualTo(old?.isShippingClassSectionVisible) { isVisible ->
                product_shipping_class_spinner.isVisible = isVisible
            }
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isVisible ->
                doneButton?.isVisible = isVisible
            }
            new.shippingData.weight?.takeIfNotEqualTo(old?.shippingData?.weight) { weight ->
                showValue(product_weight, R.string.product_weight, weight, viewModel.parameters.weightUnit)
            }
            new.shippingData.length?.takeIfNotEqualTo(old?.shippingData?.length) { length ->
                showValue(product_length, R.string.product_length, length, viewModel.parameters.dimensionUnit)
            }
            new.shippingData.width?.takeIfNotEqualTo(old?.shippingData?.width) { width ->
                showValue(product_width, R.string.product_width, width, viewModel.parameters.dimensionUnit)
            }
            new.shippingData.height?.takeIfNotEqualTo(old?.shippingData?.height) { height ->
                showValue(product_height, R.string.product_height, height, viewModel.parameters.dimensionUnit)
            }
            new.shippingData.shippingClassId?.takeIfNotEqualTo(old?.shippingData?.shippingClassId) { classId ->
                product_shipping_class_spinner.setText(viewModel.getShippingClassByRemoteShippingClassId(classId))
            }
        }
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(ProductShippingFragment.KEY_SHIPPING_DIALOG_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                    requireActivity(),
                    event.positiveBtnAction,
                    event.negativeBtnAction,
                    event.messageId
                )
                else -> event.isHandled = false
            }
        })
    }

    private fun setupViews() {
        product_weight.setOnTextChangedListener {
            viewModel.onDataChanged(weight = editableToFloat(it))
        }
        product_length.setOnTextChangedListener {
            viewModel.onDataChanged(length = editableToFloat(it))
        }
        product_height.setOnTextChangedListener {
            viewModel.onDataChanged(height = editableToFloat(it))
        }
        product_width.setOnTextChangedListener {
            viewModel.onDataChanged(width = editableToFloat(it))
        }
        product_shipping_class_spinner.setClickListener {
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
                    productShippingClassSlug = viewModel.shippingData.shippingClassSlug ?: ""
                )
        findNavController().navigateSafely(action)
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.PRODUCT_SHIPPING_CLASS -> {
                val selectedShippingClassSlug = result.getString(ARG_SELECTED_SHIPPING_CLASS_SLUG, "")
                val selectedShippingClassId = result.getLong(ARG_SELECTED_SHIPPING_CLASS_ID)
                viewModel.onDataChanged(
                    shippingClassSlug = selectedShippingClassSlug,
                    shippingClassId = selectedShippingClassId
                )
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
