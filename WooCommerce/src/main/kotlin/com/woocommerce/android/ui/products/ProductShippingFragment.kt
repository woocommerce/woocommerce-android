package com.woocommerce.android.ui.products

import android.os.Bundle
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
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
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
                // TODO viewModel.onDoneButtonClicked(ExitShipping(shouldShowDiscardDialog = false))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO
        return true
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                        requireActivity(),
                        event.positiveBtnAction,
                        event.negativeBtnAction
                )
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

        val weightUnit = viewModel.parameters?.weightUnit
        val dimensionUnit = viewModel.parameters?.dimensionUnit

        showValue(product_weight, R.string.product_weight, productData.product?.weight, weightUnit)
        showValue(product_length, R.string.product_length, productData.product?.length, dimensionUnit)
        showValue(product_height, R.string.product_height, productData.product?.height, dimensionUnit)
        showValue(product_width, R.string.product_width, productData.product?.width, dimensionUnit)

        product_shipping_class_spinner.setText(productData.product?.shippingClass ?: "")
        product_shipping_class_spinner.setClickListener {
            showShippingClassFragment()
        }
    }

    private fun showShippingClassFragment() {
        val action = ProductShippingFragmentDirections.actionProductShippingFragmentToProductShippingClassFragment(
                shippingClassSlug = product_shipping_class_spinner.getText()
        )
        findNavController().navigate(action)
    }
}
