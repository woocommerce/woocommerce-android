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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitShipping
import com.woocommerce.android.ui.products.ProductShippingClassFragment.Companion.ARG_SELECTED_SHIPPING_CLASS_ID
import com.woocommerce.android.ui.products.ProductShippingClassFragment.Companion.ARG_SELECTED_SHIPPING_CLASS_SLUG
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import kotlinx.android.synthetic.main.fragment_product_shipping.*
import org.wordpress.android.util.ActivityUtils

/**
 * Fragment which enables updating product shipping data.
 */
class ProductShippingFragment : BaseProductFragment(), NavigationResult {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        initListeners()
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_settings)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(ExitShipping(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked(ExitShipping())

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitShipping -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        updateProductView(viewModel.getProduct())
    }

    private fun initListeners() {
        fun editableToFloat(editable: Editable?): Float {
            val str = editable?.toString() ?: ""
            return if (str.isEmpty()) 0.0f else str.toFloat()
        }

        product_weight.setOnTextChangedListener {
            viewModel.updateProductDraft(weight = editableToFloat(it))
            changesMade()
        }
        product_length.setOnTextChangedListener {
            viewModel.updateProductDraft(length = editableToFloat(it))
            changesMade()
        }
        product_height.setOnTextChangedListener {
            viewModel.updateProductDraft(height = editableToFloat(it))
            changesMade()
        }
        product_width.setOnTextChangedListener {
            viewModel.updateProductDraft(width = editableToFloat(it))
            changesMade()
        }
        product_shipping_class_spinner.setClickListener {
            showShippingClassFragment()
        }
    }

    /**
     * Shows the passed weight or dimension value in the passed view and sets the hint so it
     * includes the weight or dimension unit, ex: "Width (in)"
     */
    private fun showValue(view: WCMaterialOutlinedEditTextView, @StringRes hintRes: Int, value: Float?, unit: String?) {
        val valStr = if (value != 0.0f) (value?.toString() ?: "") else ""
        view.setText(valStr)
        view.hint = if (unit != null) {
            getString(hintRes) + " ($unit)"
        } else {
            getString(hintRes)
        }
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = productData.productDraft
        if (product == null) {
            WooLog.w(T.PRODUCTS, "product shipping > productData.product is null")
            return
        }

        val weightUnit = viewModel.parameters.weightUnit
        val dimensionUnit = viewModel.parameters.dimensionUnit

        showValue(product_weight, R.string.product_weight, product.weight, weightUnit)
        showValue(product_length, R.string.product_length, product.length, dimensionUnit)
        showValue(product_height, R.string.product_height, product.height, dimensionUnit)
        showValue(product_width, R.string.product_width, product.width, dimensionUnit)
        product_shipping_class_spinner.setText(
                viewModel.getShippingClassByRemoteShippingClassId(product.shippingClassId)
        )
    }

    private fun showShippingClassFragment() {
        val action = ProductShippingFragmentDirections
                .actionProductShippingFragmentToProductShippingClassFragment(
                        productShippingClassSlug = viewModel.getProduct().productDraft?.shippingClass ?: ""
                )
        findNavController().navigate(action)
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            RequestCodes.PRODUCT_SHIPPING_CLASS -> {
                val selectedShippingClassSlug = result.getString(ARG_SELECTED_SHIPPING_CLASS_SLUG, "")
                val selectedShippingClassId = result.getLong(ARG_SELECTED_SHIPPING_CLASS_ID)
                viewModel.updateProductDraft(
                        shippingClass = selectedShippingClassSlug,
                        shippingClassId = selectedShippingClassId
                )
                product_shipping_class_spinner.setText(
                        viewModel.getShippingClassByRemoteShippingClassId(selectedShippingClassId)
                )
                changesMade()
            }
        }
    }
}
