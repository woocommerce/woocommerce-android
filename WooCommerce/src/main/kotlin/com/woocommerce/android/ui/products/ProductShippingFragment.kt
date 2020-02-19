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
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitShipping
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import kotlinx.android.synthetic.main.fragment_product_shipping.*
import org.wordpress.android.util.ActivityUtils

/**
 * Fragment which enables updating product shipping data.
 */
class ProductShippingFragment : BaseProductFragment() {
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

    /**
     * Shows the passed weight or dimension value in the passed view and sets the hint so it
     * includes the weight or dimension unit, ex: "Width (in)"
     */
    private fun showValue(view: WCMaterialOutlinedEditTextView, @StringRes hintRes: Int, value: Float?, unit: String?) {
        view.setText(value?.toString() ?: "")
        view.setHint(if (unit != null) {
            getString(hintRes) + " ($unit)"
        } else {
            getString(hintRes)
        })
    }

    private fun updateProductView(productData: ProductDetailViewState) {
        if (!isAdded) return

        val product = productData.product
        if (product == null) {
            WooLog.w(T.PRODUCTS, "product shipping > productData.product is null")
            return
        }

        val weightUnit = viewModel.parameters?.weightUnit
        val dimensionUnit = viewModel.parameters?.dimensionUnit

        fun toFloatOrNull(editable: Editable?): Float? {
            val str = editable?.toString() ?: ""
            return if (str.isEmpty()) {
                null
            } else {
                str.toFloat()
            }
        }

        with(product_weight) {
            showValue(this, R.string.product_weight, product.weight, weightUnit)
            setOnTextChangedListener {
                viewModel.updateProductDraft(weight = toFloatOrNull(it))
            }
        }
        with(product_length) {
            showValue(this, R.string.product_length, product.length, dimensionUnit)
            setOnTextChangedListener {
                viewModel.updateProductDraft(length = toFloatOrNull(it))
            }
        }
        with(product_height) {
            showValue(this, R.string.product_height, product.height, dimensionUnit)
            setOnTextChangedListener {
                viewModel.updateProductDraft(height = toFloatOrNull(it))
            }
        }
        with(product_width) {
            showValue(this, R.string.product_width, product.width, dimensionUnit)
            setOnTextChangedListener {
                viewModel.updateProductDraft(width = toFloatOrNull(it))
            }
        }
        with(product_shipping_class_spinner) {
            setText(product.shippingClass)
            setClickListener {
                showShippingClassFragment()
            }
        }
    }

    private fun showShippingClassFragment() {
        val action = ProductShippingFragmentDirections.actionProductShippingFragmentToProductShippingClassFragment(
                shippingClassSlug = product_shipping_class_spinner.getText()
        )
        findNavController().navigate(action)
    }
}
